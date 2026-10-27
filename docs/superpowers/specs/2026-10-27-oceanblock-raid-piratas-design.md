# Raid de piratas — design

Data: 2026-10-27. Pedido do usuário: "Raid de piratas".

## Intenção

Um evento raro e empolgante na ilha: de vez em quando, sem aviso prévio, barcos de piratas surgem no mar e atacam. É um combate curto, com HUD próprio, piratas brilhando e drops valiosos (diamante, maçã dourada, moedas que viram coins, pérolas, XP). Vencer paga um bônus; perder não tira nada do jogador.

## Decisões do usuário

- Gatilho aleatório: a cada 10 min com membro da ilha online **dentro da ilha**, 3% de chance; no máximo **1 raid por dia por ilha**.
- Ilha só sofre raid se algum membro online ali tiver **nível 10+**.
- Piratas chegam em **barcos vanilla** (versão inicial; um navio construído pode vir depois).
- Tripulação: **Pillager** (besta) e **Vindicator** (machado). Sem evoker nem ravager.
- **3 ondas, 5 min no total**; a próxima onda chega quando a anterior morre. Vencer = matar as 3.
- Dificuldade: **mais piratas conforme o nível**; vida e dano vanilla.
- Prêmios: **drops dos piratas + bônus de vitória** (coins + XP de nível + toast).
- Moeda física: cai no chão e, ao pegar, vira coins; só **dono, membros e confiados** pegam; some em 2 min.
- Moeda com **textura própria** (arte original).
- Derrota: tempo acaba ou a ilha fica sem membros, os piratas fogem (somem), sem bônus, sem perder nada.
- Avisos: **toast D1** no início e no fim e **som de corneta** a cada onda.
- HUD de invasão (tempo, onda, piratas vivos) **substitui temporariamente o HUD da missão**; a missão volta quando a raid acaba.
- Piratas **brilham** (contorno `glowing`) e **não quebram nada** na ilha.
- Na **última onda** vem um **Super Pirata** que **brilha dourado** e dropa também um **Totem da Imortalidade**.

## Escolhas minhas (o usuário pode trocar)

- A pérola também cai como item físico próprio que vira a moeda `perolas` ao ser pega, igual à moeda pirata (arte original, mesma regra de quem pega).
- O sorteio é global a cada 12.000 ticks (10 min) sobre as ilhas que têm membro online dentro do lote; a ilha não precisa ter ficado 10 min ocupada.
- "Membro" no gatilho, no HUD e nos prêmios = dono, membro ou confiado (`CollectorRule`-like: `ownerUuid`, `isMember`, `isTrustedMember`). Visitantes não veem HUD, não pegam moedas e não ganham bônus, mas podem lutar.
- Nível usado na dificuldade: o maior nível entre os membros online dentro do lote no início da raid.
- Comando admin `/piratas iniciar [jogador]` (força uma raid na ilha onde o jogador está, ignora chance, nível e limite diário e **não** gasta a raid do dia) e `/piratas parar` (encerra como fuga). Permissão `oneblock.admin`.
- A vitória paga o bônus a cada membro online dentro do lote no momento da vitória.
- Matar piratas também dá XP de nível pelo `XpListener` existente (`mobs.tipos.PILLAGER/VINDICATOR: 5` em `nivel.yml`) e o XP vanilla de orbes; nada muda nisso.
- Cor do brilho: piratas comuns em vermelho, Super Pirata em dourado. A cor do `glowing` vem da cor do time do scoreboard principal; o sidebar usa FastBoard (pacotes) e não troca o scoreboard do jogador, então dois times no scoreboard principal (`oceanblock_piratas` RED e `oceanblock_superpirata` GOLD, sem colisão alterada, nome visível) bastam.
- Super Pirata: Vindicator `Super Pirata` (`<gold>`), vida 80, armadura completa de ferro e machado de diamante (drop 0), dano vanilla; chega no próprio barco junto com a onda 3 (além dos piratas da faixa) e conta como pirata vivo da onda.
- Reiniciar o servidor no meio de uma raid a cancela: `onDisable` remove piratas, barcos e itens de raid; a raid do dia continua gasta.

## Fluxo

1. **Sorteio** (`RaidRoll`, puro): elegível se a ilha não tem raid ativa, `ultimoDia != hoje` (data em `America/Sao_Paulo`), algum membro online no lote tem nível ≥ `nivel-minimo` (10). Então `random < chance` (0.03).
2. **Início**: grava `ultimoDia = hoje` (write-through), monta a onda 1, toca a corneta, mostra o toast `Piratas à vista!` / `Defenda sua ilha` e o HUD, suspende o HUD de missão de cada membro no lote.
3. **Onda**: os barcos nascem em ângulos aleatórios num círculo de raio `min(barcos.distancia, borda/2 - 3)` (32, ou 22 com a borda inicial de 50, para os barcos nascerem dentro da borda da ilha) em torno do centro da ilha, na superfície do mar (Y da água, `IslandPlacement.GROUND_Y - 1` como referência, procurando a água na coluna). Só nascem em chunk carregado; sem chunk carregado no círculo, o raio cai até 16. Cada barco leva 2 piratas.
4. **Navegação**: a cada tick o barco recebe velocidade horizontal `barcos.velocidade` (0.25 bloco/tick) em direção ao centro (`BoatPath`, puro). Ao encostar (bloco sólido à frente na altura da água, ou dentro de `barcos.desembarque` = 8 blocos do centro, ou parado por 3 s), os piratas desembarcam e o barco é removido 5 s depois com partículas de splash.
5. **Combate**: piratas têm `FOLLOW_RANGE` 48, alvo inicial = membro mais próximo no lote; IA vanilla depois. Um pirata que se afasta mais que `limite` (64 blocos) do centro por 10 s volta (teleporte) para um ponto da costa.
6. **Onda limpa**: quando os piratas vivos da onda chegam a 0, a próxima onda nasce na hora (corneta de novo). Depois da onda 3: **vitória**.
7. **Fim**:
   - Vitória: toast `Raid vencida!` / `+N Coins`, bônus pago, som de level up.
   - Tempo esgotado (`duracao-segundos` 300): toast `Os piratas fugiram` / `Mais sorte na próxima`, piratas e barcos somem com fumaça.
   - Ilha sem membros no lote por 30 s seguidos: piratas somem, sem toast (ninguém para ver), sem bônus.
   - Em todos os casos o HUD de invasão some e o de missão volta.

## Ondas e dificuldade (`piratas.yml`)

Faixas por nível mínimo; vale a maior faixa ≤ nível da raid:

| faixa | piratas por onda | brutamontes |
|---|---|---|
| 10 | 3 / 4 / 5 | 0% |
| 20 | 4 / 5 / 6 | 25% |
| 30 | 5 / 6 / 8 | 35% |
| 40 | 6 / 8 / 10 | 50% |

`brutamontes` = fração de piratas que são Vindicator (arredondada, sorteando quem); o resto são Pillagers. Barcos = `ceil(piratas / 2)`.

## Piratas

- Pillager: nome `Pirata` (vermelho), besta na mão. Vindicator: nome `Pirata Brutamontes`, machado de ferro.
- `isGlowing = true`, `removeWhenFarAway = false`, `isPersistent = false`, `isPatrolLeader = false`, `canJoinRaid = false`, chance de drop de equipamento 0.
- Marcados no PDC com `oceanblock:pirata` = uuid da ilha e no mapa em memória da raid.
- Sem griefing: `EntityChangeBlockEvent` e `EntityBreakDoorEvent` cancelados para piratas.
- Morte: os drops vanilla são limpos (nada de besta, esmeralda ou garrafa sinistra) e trocados pela tabela da raid; orbes de XP vanilla ficam.

## Drops por pirata (`drops` em `piratas.yml`)

Cada entrada rola independente (`material`/`especial`, `chance`, `min`, `max`):

| item | chance | qtd |
|---|---|---|
| moeda pirata | 100% | 1 |
| pérola | 20% | 1–2 |
| diamante | 10% | 1 |
| maçã dourada | 8% | 1 |
| ferro | 30% | 1–3 |
| ouro | 15% | 1–2 |
| esmeralda | 10% | 1–2 |

Vindicator rola a tabela duas vezes (só a moeda não dobra). O Super Pirata (`super-pirata` em `piratas.yml`) dropa sempre 1 `TOTEM_OF_UNDYING`, rola a tabela 3 vezes e solta 5 moedas. Valor da moeda: sorteio entre `moeda.min` e `moeda.max` (150–400) vezes o fator de nível `1 + 0.05 × (nível - 10)` (nível 50 = 3×), arredondado.

## Moeda e pérola físicas

- Itens Textura planos `oneblock:moeda_pirata` e `oneblock:perola_pirata` (`configs/items/piratas.yml`, PNGs originais 16×16 em `textures/item/piratas/`), base `GOLD_NUGGET` / `PRISMARINE_CRYSTALS` com `item_model`; o valor vai no PDC (`oceanblock:raid_moeda` = coins, `oceanblock:raid_perola` = pérolas). Nome `Moeda pirata` / `Pérola`, item brilhando.
- `EntityPickupItemEvent`: jogador membro da ilha do item (uuid da ilha também no PDC) → cancela, remove a entidade, deposita (`Currencies.deposit`), som de pickup e linha `+N Coins` / `+N Pérolas` na região `coleta` (mesmo estilo do `CollectNotifier`). Não membro ou mob → cancela.
- `InventoryPickupItemEvent` (funil) cancelado para esses itens.
- Some em 2 min (`ticksLived` ajustado para faltar 2.400 ticks).
- Se alguém conseguir o item no inventário (creative, comando), ele não faz nada; não há uso nem venda.

## Bônus de vitória

Por membro online no lote: `vitoria.coins` (3.000) × fator de nível da raid em coins, e XP de nível = `round(vitoria.xp-fracao × toNext(nível do jogador))` (0.2) via `PlayerLevels.award`.

## HUD de invasão

- Slot Textura `invasao`, texto puro de `RaidHudText`: `Invasão pirata · Onda 2/3 · 5 piratas · 3:12`. Atualizado a cada 20 ticks e a cada morte.
- Mostrado a todo membro dentro do lote; quem entra no lote durante a raid recebe, quem sai perde (checado a cada 20 ticks).
- **Missões**: o módulo `missions` ganha `MissionHud.suspend(player)` / `resume(player)`. Suspenso, `show` esconde o slot `missao` e não redesenha; `resume` redesenha com o estado atual. Banners de conclusão continuam na fila. O raid depende `compileOnly` de `missions`.

## Avisos

- Toast D1 na região `toasts`, chaves `piratas_0`/`piratas_1`, 100 ticks, `ToastLine`s de `RaidCard` (puro):
  - Início: `Piratas à vista!` (`<red>`) com backdrop novo `glyph:toast_pirata` + `Defenda sua ilha` (`<white>`, `glyph:toast_linha`).
  - Vitória: `Raid vencida!` (`<red>`, mesmo selo) + `+N Coins`.
  - Fuga: `Os piratas fugiram` + `Mais sorte na próxima`.
- `toast_pirata`: 145×26 como os outros D1 (`textures/icons/piratas/`, `configs/glyphs/piratas.yml`), selo 26×26 vermelho #C8413A com caveira pirata desenhada à mão em pixel art (arte original), corpo #101010 a 84%.
- Som `EVENT_RAID_HORN` no início de cada onda para os membros no lote.

## Persistência

Coleção `pirate_raids`, doc `PirateRaidDay(islandUuid: String, lastDay: String)`, índice único em `islandUuid`. Carregada no `init`; gravada write-through no início de cada raid sorteada (no máximo uma por ilha por dia, então sem flush periódico). Raid em andamento vive só em memória.

## Módulo

`plugins/oceanblock/system/pirate-raid`, pacote `ocean_block.system.pirate_raid`, `PirateRaidSystem.init(plugin, paperCommandManager)` depois de `PlaytimeRewardsSystem`; `PirateRaidSystem.onDisable()` em `OceanBlockPlugin.onDisable()`. Depende `compileOnly` de `textura`, `lib:connection`, `lib:instance`, `lib:formatter`, `islands`, `level`, `currencies`, `missions`. Config `server/plugins/ocean-block/piratas.yml` copiada do bundle quando falta, lida uma vez; parser puro registra erros sob `[piratas]` e cai no padrão.

## Testes (Kotest, regras puras)

`RaidConfigParser`, `RaidRoll`, `WaveTable` (faixa, composição, barcos), `LootRoller` (com `Random` injetado), `CoinValue`, `BoatPath` (ponto no círculo, passo para o centro, desembarque), `RaidState` (onda limpa → próxima / vitória, tempo esgotado, ilha vazia), `RaidHudText`, `RaidCard`. Parte Bukkit (barcos, IA, pickup, HUD) vai para uma seção nova do checklist manual.

## Não objetivos

- Navio construído (schematic), evoker, ravager.
- Piratas roubando itens ou coins.
- Raid em visita a ilha alheia sem membros.
- Agendar raid ou escolher hora.

## Anti-objetivos

- Piratas quebrando blocos ou máquinas.
- Raid repetida no mesmo dia na mesma ilha (fora o comando admin).
- Moeda que visitantes conseguem pegar.
- HUD de missão e de invasão ao mesmo tempo.
