# Brocas do Minerador Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task (the user wants the work in the main thread, no subagents). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Brocas de ferro (embutida), ouro, diamante, obsidiana e netherite que mudam a velocidade do Minerador e a cor da broca que gira.

**Architecture:** Regras puras (`DrillTier`, `MinerRules`, `MinerCrackRules`) decidem ganho de progresso, energia e rachaduras por nível; o nível vive no counter `minerador_broca`. `DrillItem` (um `CustomItem` por nível) é encaixado pelo `MinerMenu`; `MinerDisplay` troca o modelo da broca pelo nível.

**Tech Stack:** Kotlin 2.3, Paper 26.3, inventory-framework 3.7.1, Kotest 5.9, Textura resource pack.

**Spec:** `docs/superpowers/specs/2026-10-24-oceanblock-brocas-minerador-design.md`

## Global Constraints

- Sem comentários no código; textos ao jogador em pt-BR.
- Ticks por bloco: ferro 40, ouro 30, diamante 20, obsidiana 15, netherite 10. Um bloco = 120 unidades de progresso; ganho por ciclo de 10 ticks = `10 × 120 / ticksPorBloco`.
- Energia: 40 E por bloco, por ciclo `ceil(40 × ganho / 120)` = 10, 14, 20, 27, 40.
- Counter `minerador_broca` (nível 0..4); ausente ou fora da faixa = ferro.
- Ids dos itens: `broca_ouro`, `broca_diamante`, `broca_obsidiana`, `broca_netherite`. Modelos: `oneblock:minerador_broca` (ferro) e `oneblock:minerador_broca_{ouro,diamante,obsidiana,netherite}`.
- Receitas sem pesquisa (`research = null`), categoria `MAQUINAS`.
- Arte própria; nenhum asset de Create ou Better 3D Blocks.
- Commits `type(scope): descrição` terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

- Encaixar com o inventário cheio: a broca anterior cai nos pés, nunca some.
- Tirar a broca com o inventário cheio: nada muda e o jogador é avisado.
- Encaixar a mesma broca já encaixada: não consome nem devolve nada.
- Pilha de brocas no inventário: encaixar tira só uma.
- Minerador antigo sem counter: roda como ferro, com o mesmo ritmo e gasto de antes (2 s, 10 E por ciclo).

---

### Task 1: DrillTier e progresso por nível

**Files:**
- Create: `plugins/oceanblock/system/machines/src/main/kotlin/ocean_block/system/machines/miner/DrillTier.kt`
- Modify: `.../miner/MinerRules.kt`
- Test: `.../test/.../miner/DrillTierTest.kt`, `.../miner/MinerRulesTest.kt`

**Interfaces:**
- Produces: `enum class DrillTier(val level: Int, val label: String, val ticksPerBlock: Int, val itemId: String?, val model: String)` com `gain: Int`, `cycleEnergy: Long`; `DrillTier.of(level: Long?): DrillTier`; `DrillTier.byItemId(id: String?): DrillTier?`; `DrillTier.KEY = "minerador_broca"`.
- Produces: `MinerRules.BLOCK_UNITS = 120`, `MinerRules.advance(progress: Int, gain: Int): Int`, `MinerRules.isDue(progress: Int): Boolean`, `MinerRules.afterBreak(progress: Int): Int`. Remove `INTERVAL`, `cycleEnergy(ticks)`.

- [ ] Testes: `of(null)`, `of(-1)`, `of(9)` = IRON; `of(2)` = DIAMOND; ganhos 30/40/60/80/120; energia 10/14/20/27/40; `byItemId("broca_ouro")` = GOLD, `byItemId(null)` e `byItemId("minerador")` = null, IRON sem item; modelos. MinerRules: ferro 4 ciclos até `isDue`; obsidiana 3 ciclos → 2 blocos (`advance` 80 → 160 due → `afterBreak` 40 → 120 due → 0); sem bloco o progresso para em `120 + ganho - 1`.
- [ ] Rodar `./gradlew :plugins:oceanblock:system:machines:test --tests "*miner*"`: FAIL.
- [ ] Implementar. `advance = minOf(progress + gain, BLOCK_UNITS + gain - 1)`, `afterBreak = (progress - BLOCK_UNITS).coerceAtLeast(0)`.
- [ ] Rodar de novo: PASS.

### Task 2: Rachaduras por nível

**Files:** Modify `miner/MinerCrackRules.kt`, `miner/MinerCracks.kt`, test `MinerCrackRulesTest.kt`.

**Interfaces:**
- Produces: `MinerCrackRules.progress(paid: Int, gain: Int, sinceTicks: Int): Int` = `minOf(paid + gain * sinceTicks.coerceIn(0, CYCLE_TICKS) / CYCLE_TICKS, minOf(paid + gain, BLOCK_UNITS))`; `stage(progress)` sobre `BLOCK_UNITS`. `MinerCracks.update(miner, block, progress, gain)`.

- [ ] Testes: ferro `progress(30, 30, 5)` = 45; `progress(90, 30, 25)` = 120; ouro `progress(80, 40, 10)` = 120; `stage(0)` 0, `stage(60)` 5, `stage(119)` 9, `stage(120)` 9.
- [ ] FAIL, implementar, PASS.
- [ ] Commit tasks 1–2: `feat(machines): niveis de broca no ritmo do minerador`.

### Task 3: Item, troca e Minerador

**Files:**
- Create: `miner/DrillItem.kt`, `miner/DrillSwap.kt`, test `DrillSwapTest.kt`
- Modify: `miner/MinerBlock.kt`, `miner/MinerDisplay.kt`, `miner/MinerMenu.kt`, `MachinesSystem.kt`

**Interfaces:**
- Produces: `class DrillItem(val tier: DrillTier) : CustomItem` (PAPER, `itemModel` = modelo do nível, nome `<tier cor>Broca de <label>`, lore com tempo por bloco e E/s); `DrillItem.all: List<DrillItem>`; `DrillItem.of(tier): DrillItem?`.
- Produces: `DrillSwap.mount(current: DrillTier, inserted: DrillTier): DrillSwap.Result?` (null quando igual) com `Result(next: DrillTier, returned: DrillTier?)` (returned null para ferro); `DrillSwap.unmount(current): Result?` (null quando ferro).
- Produces: `MinerBlock.tier(placed): DrillTier`, `MinerBlock.setTier(placed, tier)` (saveNow).
- Produces: `MinerDisplay.ensure(placed, block)` usa `MinerBlock.tier(placed).model` e troca o item do display da broca quando o nível muda.

- [ ] Testes `DrillSwap`: ferro + ouro → (ouro, null); ouro + diamante → (diamante, ouro); ouro + ouro → null; unmount ouro → (ferro, ouro); unmount ferro → null.
- [ ] FAIL, implementar, PASS.
- [ ] `MinerBlock.work`: ganho e energia de `tier(placed)`; `afterBreak` no lugar de zerar; lore do bloco cita a broca de ferro e o slot.
- [ ] `MinerMenu`: layout `"  L B C I "`; `B` mostra a broca (ícone `DrillItem` ou `IRON_PICKAXE` para ferro) com tempo por bloco, E/s e a ação; clique tira via `unmount` (inventário cheio: aviso, nada muda); `onClick` no inventário do jogador encaixa uma unidade via `mount`, devolve a anterior (sobra cai nos pés), som, `click.update()`. Alavanca mostra o E/s da broca.
- [ ] `MachinesSystem.init`: `DrillItem.all.forEach(CustomItemRegistry::register)`.
- [ ] `./gradlew :plugins:oceanblock:plugin:localDeploy` compila.

### Task 4: Receitas

**Files:** Modify `recipe/MachineRecipes.kt`.

- [ ] Registrar `broca_ouro` (2 `GOLD_BLOCK`, 4 ferro, 4 redstone), `broca_diamante` (custom `broca_ouro` 1, 6 `DIAMOND`, 4 redstone), `broca_obsidiana` (custom `broca_diamante` 1, 8 `OBSIDIAN`, 2 diamantes), `broca_netherite` (custom `broca_obsidiana` 1, 2 `NETHERITE_INGOT`), categoria `MAQUINAS`, `research = null`.
- [ ] Teste existente de categorias continua passando; compilar.
- [ ] Commit tasks 3–4: `feat(machines): brocas encaixaveis no minerador`.

### Task 5: Arte das brocas

**Files:** Create em `server/plugins/Textura/resource/assets/oneblock/`: `textures/block/maquinas/minerador_broca_{ouro,diamante,obsidiana,netherite}.png`, `models/block/minerador_broca_<m>.json` (parent `oneblock:block/minerador_broca`, `textures.bit` e `particle`), `items/minerador_broca_<m>.json`.

- [ ] Script descartável (Node, no scratchpad) recolore a textura atual de ferro por luminância para a paleta de cada material (netherite com friso dourado).
- [ ] Conferir as 4 texturas ampliadas.
- [ ] Commit: `feat(textura): modelos e texturas das brocas do minerador`.

### Task 6: Docs e instalação

- [ ] CLAUDE.md (Machines › Minerador, Progression, Testing) e seção 12 do manual de aceite.
- [ ] Avisar a sessão de máquinas, `docker compose stop servidor`, `localDeploy` dos dois plugins, `docker compose start servidor`, log com `Done (` e `Hash:` sem erros novos.
- [ ] Commit: `docs(machines): brocas do minerador`.
