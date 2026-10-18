# OceanBlock — Visão, Decomposição e Design dos Subprojetos 0 e 1

Data: 2026-10-18
Origem: fork do repositório `one-block` (commit `58b2318`), Paper 26.3 / Kotlin 2.3 / JDK 25.
Escopo deste documento: visão do modo, decisões já tomadas, decomposição em subprojetos e design detalhado dos subprojetos 0 (fork base) e 1 (oceano + lote do raft). Os subprojetos 2–5 terão specs próprias.

---

## 1. Visão

Novo modo de jogo inspirado em **FTB OceanBlock** e **Raft**. O jogador nasce numa pequena balsa (raft) num oceano infinito. Recursos derivam pela água passando pelo raft e são puxados com um gancho. O raft cresce com qualquer bloco que o jogador consiga. A progressão combina **fases do mar** (o que deriva) com **tecnologia** (estações desbloqueadas numa Mesa de Pesquisa).

### 1.1 Decisões tomadas

| Tema | Decisão |
|---|---|
| Reuso do one-block | Fork separado (clone local do repo, histórico preservado) |
| Base do jogador | Raft inicial fixo, expansível com **qualquer bloco** (sem exigência de fundação) |
| Recursos | Continuam derivando e podem ser puxados durante todo o jogo |
| Motor de progressão | Fases do mar + tecnologia |
| Desbloqueio de tecnologia | Mesa de Pesquisa (estilo Raft), desbloqueio por time do raft, pesquisável limitado pela fase |
| Geração do mundo | Oceano vanilla, sem estruturas |
| Fundo do mar | Vanilla, incluindo pedra e minérios (mergulhar fundo é a recompensa) |
| Multiplayer | Grade de lotes com borda pessoal (modelo das ilhas do one-block) |
| Detritos | Display entities numa corrente que cruza o lote; gancho = vara custom; barris com loot da fase; rede coleta passiva depois |
| Ameaças / sobrevivência | Nenhuma na v1 (sem tubarão, sem sede, sem eventos) |
| Versão | Paper 26.3 (a mesma do one-block, já a mais recente) |

2026-10-18: a base inicial passou de raft para ilha (`templates/island.schem`, colada em Y 64 no centro do lote).

### 1.2 Sistemas herdados

**Mantidos:** economia (`currencies`, `shop`, `sell`, `auction-house`, `trader`, `coinflip`, `tops`), progressão paralela (`collections`, `battle-pass`, `enchants`, `craft`), `playtime`, `starter-kit`, `scoreboard`.

**Mantidos como infraestrutura:** `islands` (vira o raft), `worlds`, `spawn`, `custom-items` (só o framework; gancho e estações serão construídos nele), todos os `lib:*`, `plugins/textura`.

**Removidos:** `arena`, `combat-log`, `fly-timer`, `xp-bottle`, `minions`, `spawners`, `mob-drops`, `repair`, `special-items`, `chats`, `skills` (removido em 2026-10-18 a pedido do usuário).

---

## 2. Decomposição

Cada subprojeto tem seu próprio ciclo spec → plano → implementação.

| # | Subprojeto | Entrega | Depende de |
|---|---|---|---|
| 0 | Fork base | Repo `ocean-block` podado, renomeado, build verde, servidor sobe | — |
| 1 | Oceano + lote do raft | Mundo só-oceano, lote com borda, raft inicial, spawn no raft, expansão livre, reset que restaura o mar | 0 |
| 2 | Deriva + gancho | Corrente por lote, detritos display entity, item gancho, barris com loot | 1 |
| 3 | Fases do mar | Motor de fases adaptado: meta de avanço e pools de detritos/loot por fase | 2 |
| 4 | Tecnologia | Mesa de Pesquisa + estações (rede, fundição, peneira, ...) | 2, 3 |
| 5 | Integração | Retunar skills, collections, battle-pass, shop/sell e trader para o conteúdo do mar | 3, 4 |

Ordem 0 → 1 → 2 entrega cedo algo jogável: nascer no raft e puxar detritos.

---

## 3. Subprojeto 0 — Fork base

### 3.1 Origem

- O one-block tem trabalho não commitado (~5000 entradas no `git status`, incluindo código de `plugins/` e `server/`), então um `git clone` perderia o estado atual. O fork é uma **cópia do diretório inteiro, incluindo `.git`**, de `E:\Minecraft  System\one-block` para `E:\Minecraft  System\ocean-block` (sem `build/`, `.gradle/`, `.kotlin/`, mundos e logs). O one-block não é tocado.
- No fork, o estado copiado vira um commit de snapshot (`chore: snapshot do one-block para o fork ocean-block`). O histórico até `58b2318` continua comum aos dois repositórios; o one-block é adicionado como remote `upstream` para `git cherry-pick` de correções.
- Este documento é copiado para `ocean-block/docs/superpowers/specs/` e commitado como primeiro commit do fork.

### 3.2 Poda de módulos

Para cada módulo removido (lista em 1.2): sai de `settings.gradle.kts`, a pasta é apagada, e as chamadas em `setupSystems()`, `setupCommands()`, `setupListeners()` e `onDisable()` do plugin são removidas. Configs desses módulos em `server/plugins/` também saem. A linha duplicada `include("plugins:oneblock:system:spawners")` no fim do `settings.gradle.kts` sai junto.

### 3.3 Desacoplamentos necessários

| Módulo mantido | Dependência removida | Ação |
|---|---|---|
| `trader` | `fly-timer` (`ItemsRegistry` importa `one_block.system.fly_timer.Flys`) | Remove os itens de voo do registro do trader e a dependência no `build.gradle.kts` |
| `custom-items` | `minions` (via `content/vacuum_hopper/VacuumHopper.kt`) | Remove o conteúdo `vacuum_hopper` e `magnet`; mantém `multitool` e `paxel` por ora; remove a dependência de `minions` |
| `battle-pass` | Objetivo `BreakOneBlock` | Permanece compilando; o desafio que usa esse objetivo fica impossível de completar até o subprojeto 5 |

Qualquer outra referência a um módulo removido que apareça no build é tratada do mesmo jeito: cortar o uso no módulo mantido, nunca reintroduzir o módulo.

### 3.4 Renomeação

- `rootProject.name = "ocean-block"`.
- Pasta `plugins/oneblock` → `plugins/oceanblock`; acessores tipados (`projects.plugins.oneblock...`) atualizados.
- Pacote `one_block` → `ocean_block` em todo o código Kotlin, incluindo `io.github.badnotice.one_block` → `io.github.badnotice.ocean_block`. **Exceção:** o pacote do Textura é `one_block.textura` e fica como está, assim como os imports `one_block.textura.*` nos sistemas.
- Literais `"oneblock"` no código (namespaces de `NamespacedKey`, fontes `oneblock:*`, permissões `oneblock.*`, sons do pack) não mudam: são chaves do resource pack e de dados, não nome de produto.
- `OneBlockPlugin` → `OceanBlockPlugin`, `OneBlockCommand` → `OceanBlockCommand` (alias `/oceanblock`); `plugin.yml` com `name: ocean-block`.
- Data folder do plugin: `server/plugins/ocean-block/`.
- Banco MongoDB `ocean-block` (não colide com `one-block` na mesma instância).
- `plugins/textura` mantém nome e pacote (plugin independente).
- `CLAUDE.md` atualizado para os novos nomes e caminhos, e a lista de módulos podados.

### 3.5 Servidor de desenvolvimento

- `server/` é copiado sem `world*`, `logs`, `cache`, `hs_err_*`, `replay_*` e sem a data folder do one-block.
- `server-port=25566` para rodar ao lado do one-block.
- Plugins de terceiros (LuckPerms, PlaceholderAPI, SkinsRestorer, FAWE, PacketEvents, Textura) mantidos com as configs atuais.

### 3.6 Critérios de pronto

- `./gradlew :plugins:oceanblock:plugin:localDeploy` termina verde.
- Servidor sobe sem erro de carregamento do OceanBlock nem dos plugins dependentes.
- Comandos dos sistemas mantidos respondem (`/raft` ainda não existe; `/island` herdado responde).
- `grep -r "one_block"` no código Kotlin não retorna nada.

---

## 4. Subprojeto 1 — Oceano + lote do raft

### 4.1 Mundo

- Mundo do jogo usa o **gerador vanilla** com um datapack (`oceanblock-world`) que sobrescreve a density function `minecraft:overworld/continents` por uma constante bem negativa. Resultado esperado: terreno inteiro abaixo do nível do mar, profundidade de oceano profundo, biomas oceânicos variando por temperatura (quente, morno, frio, congelado), kelp, seagrass, coral e minérios mantidos.
- `generate-structures=false` em `server.properties`: sem naufrágios, ruínas oceânicas, monumentos ou portais em ruínas.
- O mundo do jogo é o mundo principal `world` (`level-name=world`), gerado do zero no primeiro boot do fork com o datapack em `world/datapacks/`. Nenhum tipo novo em `worlds`.
- Datapacks valem para todos os mundos com gerador vanilla. O lobby `spawn` (tipo `Default` em `worlds.yml`) não existe em disco no fork e nasce também como oceano; o lobby é construído à mão depois. Mundos com gerador `VoidGen` não são afetados.
- `allow-nether=false` (o end já está desligado em `bukkit.yml`).

### 4.2 Spike obrigatório antes da implementação

Primeiro passo do plano, com saída descartável:

1. Gerar um mundo na 26.3 com o datapack e confirmar visualmente: nenhuma terra emersa num raio de vários milhares de blocos, variedade de biomas oceânicos, profundidade do fundo.
2. Testar o regen de uma região de 100×100 via FAWE no mundo oceano (base do reset em 4.6).

Se a técnica do `continents` falhar, alternativa a avaliar: datapack com `noise_settings` próprio para o overworld. Se o regen do FAWE falhar, alternativa: guardar um snapshot (schematic) do lote vazio na criação e colar no reset.

### 4.3 Lote

- Reusa `Position` (grade em espiral, `GRID_SPACING = 1000`) e a borda pessoal de `Island` (`BORDER_SIZE` 30 → 50 → 70 → 100 por upgrade).
- Centro do lote na superfície do mar: o raft substitui a camada de água do topo (Y 62 com `sea_level` vanilla 63), ficando rente à água.
- Dentro da borda, o fundo vanilla é livre para minerar e construir.

### 4.4 Raft inicial

- `StarterIsland` passa a colar `templates/raft.schem` com o centro do schematic no centro do lote, no nível do mar. Conteúdo inicial: plataforma 3×3 de tábuas com um baú; o kit inicial continua sendo entregue pelo `starter-kit`.
- Removidos de `StarterIsland`: `placeFloor` (bedrock + terra), bloco central e a pintura de bioma (o lote mantém o bioma oceânico natural).
- `Island.spawn` aponta para o topo do raft. Join e `/raft ir` teleportam para lá.
- Se o schematic não existir, o raft é gerado em código como plataforma 3×3 de `OAK_PLANKS` e o erro é logado (o jogador nunca nasce na água).

### 4.5 Expansão

- Construção vanilla com qualquer bloco. Como o jogo exige face adjacente, o raft cresce a partir das bordas.
- Proteção, permissões de membro e trust do `IslandListener` / `IslandSettingListener` continuam valendo sem mudança.

### 4.6 Reset do raft

- `IslandResetService` deixa de usar `TerrainCleaner` (que zera para ar).
- Novo fluxo: regen da região do lote via FAWE (restaura água e fundo vanilla), depois cola o raft inicial e reposiciona o spawn. Hooks de reset existentes (`IslandResetHooks`) continuam sendo chamados.
- Se o spike de 4.2 escolher a alternativa de snapshot, a criação do lote salva o schematic e o reset cola esse snapshot.

### 4.7 O que sai do `islands` neste subprojeto

- Bloco central regenerável e seu listener de quebra.
- `PhaseBlocks`, `MonsterParty`, `PhaseAnimation`, `PhaseAdvancement`, `PhaseProgress` e o campo `grid`: apagados, junto com os subcomandos `setphase` e `phases`.
- O modelo de fase (`currentPhaseId`, `Phase`, `PhaseManager`, `PhaseRewarder`, `PhaseView`, YAMLs) fica **dormente**: não é apagado, apenas perde os chamadores ligados ao bloco central. O subprojeto 3 reescreve o avanço de fase.
- `IslandHud` deixa de mostrar progresso de bloco.

### 4.8 Nomes voltados ao jogador

- Internamente as classes continuam `Island*` para reduzir churn.
- Para o jogador, "Raft": comando principal `/raft` com aliases `/balsa`, `/ilha`, `/island` e `/is`; menus, mensagens e placeholders trocam "ilha" por "raft".

### 4.9 Erros e casos-limite

- Mundo oceano não carregado ao criar lote: criação abortada com mensagem ao jogador e log `severe`, nenhum dado persistido.
- Falha ao colar o schematic: fallback 3×3 em código (4.4).
- Falha no regen durante o reset: reset abortado, ilha volta ao estado anterior ao reset e o jogador recebe mensagem; nada é marcado como resetado.
- Jogador cai na água: comportamento vanilla (nada a tratar na v1).

### 4.10 Testes

- Unitários (Kotest) para a lógica pura nova: posição do raft no lote, cálculo do spawn, fallback quando o schematic falta.
- Os testes existentes do `islands` (`IslandSettingsTest`) continuam passando.
- Verificação manual no servidor de dev, roteiro no plano: criar raft, nascer em cima, construir para fora, aumentar a borda, convidar membro, resetar e ver o mar restaurado.

### 4.11 Critérios de pronto

- `/raft criar` gera um lote no oceano e o jogador aparece em cima do raft.
- O jogador constrói para fora com qualquer bloco; fora da borda não consegue.
- Upgrade de borda funciona.
- `/raft resetar` restaura água e fundo e recoloca o raft.
- Nenhuma estrutura oceânica no mundo; lobby inalterado.

---

## 5. Fora de escopo (desta spec)

- Detritos, corrente, gancho, barris (subprojeto 2).
- Fases do mar e metas de avanço (subprojeto 3).
- Mesa de Pesquisa e estações (subprojeto 4).
- Rebalanceamento de economia, skills, collections e battle-pass (subprojeto 5).
- Ameaças e sobrevivência (tubarão, sede, eventos): fora da v1.
- Raft que navega.
