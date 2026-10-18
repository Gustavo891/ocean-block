# OceanBlock — Subprojeto 2: Deriva + Gancho

Data: 2026-10-18
Depende de: subprojetos 0 e 1 (`docs/superpowers/specs/2026-10-18-oceanblock-design.md`).
Decisões: o usuário pediu "idealize e faça". Tudo abaixo é escolha do Claude, exceto o que vem da spec mestre (detritos como display entities, gancho estilo vara de pesca, barris com loot, rede passiva fica no subprojeto 4, sem ameaças na v1).

## 1. Intenção

Primeiro loop jogável do OceanBlock. Detritos passam boiando pelo raft e o jogador os puxa com um gancho para conseguir madeira, folhas, sucata e loot de barril, e com isso aumentar o raft. O ritmo é **equilibrado**: abundante nos primeiros minutos (uma tábua a cada poucos segundos ao alcance) e barril raro como momento de recompensa.

### Objetivos
- Em 2 minutos de jogo o jogador já tem madeira para crescer o raft.
- Puxar um detrito tem retorno claro: animação de puxar, som, respingo, action bar com o ganho.
- Custo de servidor baixo com muitos rafts.

### Não-objetivos
- Pools por fase (subprojeto 3), estações e rede passiva (subprojeto 4), rebalanceamento de economia/skills (subprojeto 5).
- Tubarão, clima, eventos.
- Detritos persistentes entre reinícios.

### Anti-objetivos (falha mesmo que funcione)
- Lag com vários lotes ativos.
- Visitante sem permissão coletando detritos de outro raft.
- Detritos "fantasmas" sobrando no mundo depois de restart ou quando o lote esvazia.

## 2. Mecânica

### 2.1 Corrente
- Direção única no servidor, configurável (`EAST` padrão), velocidade padrão `0.05` bloco/tick (1 bloco/s).
- Cada lote ativo tem uma trilha centrada no centro do raft: comprimento `40` blocos ao longo da corrente; o detrito nasce no início da trilha com deslocamento lateral aleatório em `[-10, +10]` e some ao passar do fim.
- Altura: `y = 62.6` (boiando na superfície; o raft fica em Y 62).

### 2.2 Lote ativo
- Um lote está ativo enquanto houver pelo menos um jogador online dentro dele (`IslandManager.getIslandAt(player.location)`).
- A cada `100` ticks, cada lote ativo com menos de `8` detritos ganha um detrito novo.
- Lote que fica inativo perde todos os seus detritos imediatamente.

### 2.3 Detrito
- Entidade `ItemDisplay` mostrando o material de exibição do tipo, escala configurável, `persistent = false`, `teleportDuration = 2` para movimento suave.
- Um único task global (a cada 2 ticks) move todos os detritos.
- **Preso no raft:** se a posição seguinte, no bloco Y 62, não for água nem ar, o detrito para (fica encostado no raft) e é removido depois de `60` s se ninguém pegar.
- No `onDisable` do plugin todos os detritos são removidos.

### 2.4 Tipos (YAML, `server/plugins/ocean-block/drift.yml`)

| id | peso | exibição | drops (material min–max, chance) |
|---|---|---|---|
| `tabua` | 50 | `OAK_PLANKS` | `OAK_PLANKS` 1–2 (1.0) |
| `folhas` | 25 | `OAK_LEAVES` | `OAK_LEAVES` 1–2 (1.0), `STICK` 1–2 (0.5), `OAK_SAPLING` 1 (0.1), `STRING` 1 (0.3) |
| `sucata` | 17 | `IRON_NUGGET` | `IRON_NUGGET` 1–3 (1.0), `RAW_COPPER` 1 (0.25) |
| `barril` | 8 | `BARREL` | `BREAD` 1–2 (0.6), `STRING` 1–3 (0.6), `KELP` 2–4 (0.5), `OAK_SAPLING` 1 (0.3), `COD` 1–2 (0.4), `IRON_INGOT` 1 (0.1), `NAME_TAG` 1 (0.02) |

Cada drop é sorteado independentemente pela sua chance; a quantidade é uniforme em `[min, max]`. Barril que não sortear nada dá `STRING` 1 (para nunca ser vazio). Material inválido no YAML é logado e ignorado, não é fatal (mesma regra das fases).

**Seam para o subprojeto 3:** a escolha do tipo passa por `DebrisPool.typesFor(island)`; a v1 devolve todos os tipos do YAML.

### 2.5 Coleta
- **Gancho:** item custom `gancho` (`FISHING_ROD`, nome `Gancho`, lore em português) no framework `custom-items`. Ao recolher a linha (`PlayerFishEvent` em `REEL_IN`, `FAILED_ATTEMPT` ou `IN_GROUND`), o detrito livre mais próximo em raio `2.0` do anzol é puxado: desliza até o jogador em 10 ticks e então é entregue. A pesca vanilla continua funcionando normalmente com o gancho.
- **Toque:** jogador a até `1.5` blocos de um detrito coleta na hora (nadando ou na borda do raft).
- **Quem pode:** dono, membro ou trust do raft daquele lote. Os outros não coletam e não recebem mensagem.
- **Entrega:** itens vão para o inventário; o que não couber cai aos pés do jogador. Som `ENTITY_ITEM_PICKUP`, partícula `SPLASH`, action bar no canal novo `Channel.Drift` (prioridade 15) com `+N Item` por material, por 40 ticks.
- Vara de pesca comum não puxa detrito.

### 2.6 Obtenção do gancho
- Entregue no primeiro login (`!player.hasPlayedBefore()`), pelo próprio sistema de deriva.
- Receita vanilla com forma de vara: `"  P" / " PS" / "P S"`, `P` = qualquer tábua (`Tag.PLANKS`), `S` = `STRING`. Resultado: o gancho. Chave `oceanblock:gancho`.
- `/citem give gancho` funciona (registro no `CustomItemRegistry`).

## 3. Arquitetura

- Módulo novo `plugins/oceanblock/system/drift`, pacote `ocean_block.system.drift`, objeto `DriftSystem.init(plugin)` e `DriftSystem.onDisable()`.
- `compileOnly` em `islands` (lote, trust), `custom-items` (registro do gancho), `lib:instance`, `lib:actionbar`, `lib:formatter`, `lib:item-builder`, `lib:coordinate`.
- `OceanBlockPlugin`: `DriftSystem.init` depois de `IslandsSystem` e `CustomItemSystem`; `DriftSystem.onDisable()` em `onDisable`.
- `lib:actionbar`: novo `Channel.Drift(15)`.
- Unidades puras (testadas com Kotest): parser da config, sorteio de tipo por peso, sorteio de drops, geometria da trilha (posição de nascimento, avanço, fim), regra de quem pode coletar.

## 4. Erros e casos-limite
- `drift.yml` ausente: o plugin grava o padrão da seção 2.4 e segue.
- `drift.yml` inválido ou sem tipos válidos: log `severe`, deriva desligada, resto do plugin funciona.
- Mundo do lote descarregado: o lote não é tratado como ativo.
- Jogador sai do lote durante o puxão: o item ainda é entregue a ele.
- Detrito cujo lote sumiu (reset, remoção): removido no tick seguinte.

## 5. Testes
- Kotest para as unidades puras da seção 3.
- Build e testes do módulo verdes.
- Aceite manual (adicionado ao roteiro de aceite): detritos passando, puxar com gancho, coletar por toque, detrito preso no raft, barril, receita, visitante sem coleta, lote vazio limpa detritos, restart sem detritos sobrando.

## 6. Valores a calibrar depois
Todos os números (velocidade, intervalo, máximo por lote, pesos, chances) ficam no `drift.yml` para ajuste sem recompilar.
