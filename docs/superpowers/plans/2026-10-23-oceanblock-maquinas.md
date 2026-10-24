# Máquinas, energia e Picareta fundidora — Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (o usuário quer execução inline na thread principal, sem subagents). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gerador de energia, Painel solar, Fornalha elétrica, Minerador (blocos com textura, energia por raio de 8 blocos) e Picareta fundidora, pesquisáveis e craftáveis.

**Architecture:** Novo módulo `system/machines`. Regras puras testadas (`energy/`, `generator/`, `solar/`, `furnace/`, `miner/`, `pickaxe/`), lado Bukkit fino em cima do framework `CustomBlock`/`PlacedBlock` (estado em `counters`). Blocos `CUSTOM` do Textura sobre note_block. Menus de baú vanilla via inventory-framework.

**Tech Stack:** Kotlin 2.3, Paper 26.3, inventory-framework 3.7.1, Kotest 5.9, Textura.

**Spec:** `docs/superpowers/specs/2026-10-23-oceanblock-maquinas-design.md`

## Global Constraints

- Nada de comentários em código; textos ao jogador em pt-BR; MiniMessage via `Mini.mm`; `ITALIC false` nos nomes.
- Commits `type(scope): descrição` terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Gradle sempre por task `local*` ou módulo sem `lib:connection` (`:plugins:oceanblock:system:machines:test` roda limpo).
- API pública do Textura sem tipos `kotlin.*` e sem parâmetros default (`PublicApiKotlinTypesTest`).
- PNG de glyph até 256 px; texturas de bloco/item 16×16.
- `config.yml` do Textura nunca é lido nem exibido.
- Tick das máquinas: `tickInterval = 10`. Raio 8,0. Gerador 2 E/tick, bateria 10.000, combustível até 64. Painel 3 E por 10 ticks, bateria 1.000, dia = `time % 24000 < 12000`. Fornalha 4 slots, 50 ticks/item, 1 E/tick por slot ativo, pilhas até 64. Minerador 40 ticks/bloco, 40 E/bloco, 27 slots. Limites: Gerador 4, Painel 8, Fornalha 4, Minerador 4.

## Review Focus

- Dois jogadores no mesmo menu clicando ao mesmo tempo: cada clique relê o estado; nada duplica (task 8/9/10 checam `PlacedBlockManager.isRegistered`).
- Máquina quebrada com carga: item dropado carrega `counters` e ao recolocar continua (framework já faz; manual de aceite cobre).
- Fonte quebrada enquanto consumidores puxam: `EnergyIndex` refeito no break e `draw` ignora uuid não registrado.
- Saída da fornalha com outro resultado no slot: slot pausa, não troca o item (teste `FurnaceRulesTest` "saida com outro item pausa").
- Minerador com bloco abaixo fora da ilha ou custom: não quebra (teste `MinerRulesTest` de lista + checagem Bukkit da ilha).

---

### Task 1: Módulo e regra de energia

**Files:**
- Modify: `settings.gradle.kts` (adicionar `":plugins:oceanblock:system:machines"` após stations)
- Create: `plugins/oceanblock/system/machines/build.gradle.kts` (cópia do de `stations`, trocando `drift` por `stations`)
- Create: `.../machines/energy/EnergyRules.kt`
- Test: `.../machines/energy/EnergyRulesTest.kt`

**Interfaces — Produces:**
- `data class EnergySource(val id: UUID, val distance: Double, val stored: Long)`
- `object EnergyRules { const val RADIUS = 8.0; const val KEY = "energia"; fun plan(needed: Long, sources: List<EnergySource>): Map<UUID, Long>? }` — `null` se insuficiente, mapa vazio se `needed <= 0`; só fontes com `distance <= RADIUS` e `stored > 0`, mais perto primeiro, empate por `id`.

- [ ] Testes: "tira da mais perto primeiro", "soma varias fontes", "insuficiente devolve null e nao tira nada", "fonte fora do raio nao conta", "empate por uuid", "zero devolve vazio".
- [ ] Rodar `./gradlew :plugins:oceanblock:system:machines:test` — FAIL.
- [ ] Implementar.
- [ ] Rodar — PASS. Commit `feat(machines): módulo de máquinas e regra de energia por raio`.

### Task 2: Gerador e Painel (regras)

**Files:** `generator/GeneratorState.kt`, `generator/GeneratorRules.kt`, `solar/SolarRules.kt` + testes.

**Interfaces — Produces:**
- `data class GeneratorState(val fuel: String?, val fuelCount: Int, val burnTicks: Int, val energy: Long)` com `toCounters(): Map<String, Long>` (`energia`, `gerador_burn`, `gerador_fuel/<MAT>`), `companion fromCounters(Map<String, Long>)`, `EMPTY`, `PREFIX = "gerador_"`.
- `object GeneratorRules { CAPACITY = 10_000L; RATE = 2; MAX_FUEL = 64; fun insertFuel(state, material: String, amount: Int, fuelTicks: (String) -> Int?): Pair<GeneratorState, Int>; fun takeFuel(state): Pair<GeneratorState, Pair<String, Int>?>; fun tick(state, ticks: Int, fuelTicks: (String) -> Int?): GeneratorState }`
- `object SolarRules { CAPACITY = 1_000L; PER_TEN_TICKS = 3; fun isDay(time: Long): Boolean; fun produce(energy: Long, ticks: Int, active: Boolean): Long }`

Tick do gerador: enquanto há ticks e `energy < CAPACITY`: se `burnTicks == 0` acende (consome 1 combustível, `burnTicks = fuelTicks`; sem combustível para); `step = min(restante, burnTicks, ceil((CAPACITY - energy) / RATE))`; soma `step * RATE` limitado à capacidade.

- [ ] Testes: carvão 1600 em 10 ticks dá 20 E e gasta 1 carvão; bateria cheia não acende nem gasta; para no teto 10.000; combustível diferente recusado; máximo 64; retirar devolve o combustível; ida e volta de counters; solar de noite não produz, de dia +3 por 10 ticks, teto 1.000; `isDay(12000) == false`, `isDay(23999) == false`, `isDay(24500) == true`.
- [ ] FAIL → implementar → PASS. Commit `feat(machines): regras do gerador e do painel solar`.

### Task 3: Fornalha elétrica (regras)

**Files:** `furnace/FurnaceState.kt`, `furnace/FurnaceRules.kt` + teste.

**Interfaces — Produces:**
- `data class FurnaceSlot(val input: String?, val inputCount: Int, val output: String?, val outputCount: Int, val progress: Int)`; `data class FurnaceState(val slots: List<FurnaceSlot>)` com `toCounters()` (`forno_in/<i>/<MAT>`, `forno_out/<i>/<MAT>`, `forno_progress/<i>`), `fromCounters`, `EMPTY` (4 slots vazios), `PREFIX = "forno_"`.
- `enum class FurnaceSlotStatus { EMPTY, WORKING, OUTPUT_FULL }`
- `object FurnaceRules { SLOTS = 4; MAX_STACK = 64; TICKS_PER_ITEM = 50; fun insert(state, material: String, amount: Int, recipes: Map<String, String>): Pair<FurnaceState, Int>; fun takeInput(state, slot: Int): Pair<FurnaceState, Pair<String, Int>?>; fun takeOutput(state, slot: Int): Pair<FurnaceState, Pair<String, Int>?>; fun status(slot: FurnaceSlot, recipes: Map<String, String>): FurnaceSlotStatus; fun energyNeeded(state, ticks: Int, recipes): Long; fun tick(state, ticks: Int, recipes): FurnaceState }`

`insert` enche primeiro slots com o mesmo material, depois slots com `input == null`, até 64 cada; material sem receita entra 0. `tick` assume energia já paga: cada slot `WORKING` soma ticks ao progresso e completa itens a cada 50 enquanto continuar `WORKING`. `energyNeeded = slots WORKING * ticks`.

- [ ] Testes: 4 slots trabalham em paralelo; 50 ticks fazem 1 item; insert prefere slot igual; sem receita recusa; saída com outro item pausa; saída 64 pausa; takeInput/takeOutput; energyNeeded com 2 ativos e 10 ticks = 20; ida e volta de counters.
- [ ] FAIL → implementar → PASS. Commit `feat(machines): regras da fornalha elétrica`.

### Task 4: Minerador e Picareta (regras)

**Files:** `miner/MinerRules.kt`, `pickaxe/AutoSmelt.kt`, `MachineIds.kt` + testes.

**Interfaces — Produces:**
- `object MinerRules { INTERVAL = 40; ENERGY_PER_BLOCK = 40L; STORAGE = 27; PROGRESS_KEY = "minerador_progress"; val MINEABLE: Set<String>; fun canMine(material: String): Boolean; fun advance(progress: Int, ticks: Int): Int /* min(progress+ticks, INTERVAL) */; fun isDue(progress: Int): Boolean }`
- MINEABLE: `COBBLESTONE, STONE, COAL_ORE, COPPER_ORE, IRON_ORE, GOLD_ORE, REDSTONE_ORE, LAPIS_ORE, EMERALD_ORE, DIAMOND_ORE` + `DEEPSLATE_` de cada minério.
- `object AutoSmelt { fun smelt(material: String): String? }` — `RAW_IRON→IRON_INGOT`, `RAW_COPPER→COPPER_INGOT`, `RAW_GOLD→GOLD_INGOT`, `COBBLESTONE→STONE`.
- `object MachineIds { GENERATOR = "gerador_energia"; SOLAR = "painel_solar"; FURNACE = "fornalha_eletrica"; MINER = "minerador"; PICKAXE = "picareta_fundidora"; NO_POWER_KEY = "sem_energia"; val blocks = listOf(GENERATOR, SOLAR, FURNACE, MINER) }`

- [ ] Testes: minera pedregulho e deepslate_diamond_ore, recusa DIRT/GRASS_BLOCK/BEDROCK; advance satura em 40; AutoSmelt dos 4 e `null` para DIAMOND.
- [ ] FAIL → implementar → PASS. Commit `feat(machines): regras do minerador e da auto-fundição`.

### Task 5: Categoria Máquinas e pesquisas

**Files:**
- Modify: `craft/.../recipe/RecipeCategory.kt` (+`MAQUINAS("Máquinas", "observer", "Máquinas que trabalham sozinhas com energia.")`), `craft/.../menu/RecipeLore.kt` (`CATEGORY_SLOTS = 4`), `craft/.../menu/RecipesMenu.kt` (layout linha 4 com `C`)
- Modify: `research/.../model/ResearchDefinition.kt` (+`MAQUINAS("Máquinas", "observer")`), `research/.../menu/ResearchLore.kt` (`CATEGORY_SLOTS = 4`), `research/.../menu/ResearchMenu.kt` (layout `C` nas linhas 1–4, `V` na linha 5 coluna 1: `"V   < >  "`)
- Modify: `research/.../tree/ResearchTree.kt` (+5 pesquisas da tabela da spec §6; ícones `item:iron_pickaxe`, `item:furnace`, `item:daylight_detector`, `item:blast_furnace`, `item:observer`; custos com chaves `iron_ingot`, `gold_ingot`, `coal`, `copper_ingot`, `redstone`, `lapis_lazuli`)
- Modify: `ResearchTreeTest.kt` (ids, ícones, "minerador requer gerador_energia")
- Modify: arte `server/plugins/Textura/resource/assets/oneblock/textures/gui/receitas_menu.png` (moldura de slot em x 7, y 71) e `pesquisa_menu.png` (moldura em x 7, y 89), copiando a moldura de cima.

- [ ] Atualizar testes (`RecipeLoreTest` e `ResearchLoreTest` já checam `entries.size <= CATEGORY_SLOTS`; `ResearchTreeTest` com os ids novos) → FAIL.
- [ ] Implementar → `./gradlew :plugins:oceanblock:system:craft:test :plugins:oceanblock:system:research:localTest` PASS.
- [ ] Editar as duas PNGs (script Java no scratchpad) e conferir lendo a imagem.
- [ ] Commit `feat(research): categoria Máquinas nos menus de receitas e pesquisa`.

### Task 6: Textura dos blocos e da picareta

**Files:**
- Modify: `plugins/textura/.../api/BlockApi.kt` (+`fun markItem(item: ItemStack, id: String)`: grava `TexturaBlockKeys.BLOCK_ID`)
- Create: `server/plugins/Textura/configs/blocks/maquinas.yml` (4 blocos `CUSTOM`, `textures: {top, bottom, side}`, `block_default_drop: false`, nomes pt-BR)
- Create: `server/plugins/Textura/configs/items/maquinas.yml` (`picareta_fundidora`, `IRON_PICKAXE`, `item_model: oneblock:picareta_fundidora`, `type: flat`)
- Create: PNGs 16×16 em `resource/assets/oneblock/textures/block/maquinas/<id>_{top,bottom,side}.png` e `resource/assets/oneblock/textures/item/maquinas/picareta_fundidora.png`, geradas por script Java descartável a partir de `generated/vanilla/client-26.3.jar`.

- [ ] Gerar as PNGs, conferir visualmente lendo cada uma.
- [ ] `./gradlew :plugins:textura:localTest` PASS (inclui `PublicApiKotlinTypesTest`).
- [ ] Commit `feat(textura): texturas das máquinas e da picareta fundidora`.

### Task 7: Base Bukkit, energia em rede e Gerador/Painel

**Files:** `MachineBlock.kt`, `energy/EnergyIndex.kt`, `energy/EnergyNetwork.kt`, `generator/GeneratorBlock.kt`, `generator/GeneratorMenu.kt`, `solar/SolarBlock.kt`, `menu/MachineMenus.kt` (itens de status, título, guarda de acesso).

**Interfaces — Produces:**
- `abstract class MachineBlock : CustomBlock()` — `material = NOTE_BLOCK`, `itemModel = NamespacedKey("oneblock", id)`, `tickInterval = 10`, `buildItem` chama `super` e `BlockApi.markItem`.
- `object EnergyIndex { fun rebuild(); fun sourcesNear(placed: PlacedBlock): List<Pair<PlacedBlock, Double>> }` (ilha via `IslandManager.getIslandAt`, fontes = blocos `GENERATOR`/`SOLAR`).
- `object EnergyNetwork { fun draw(consumer: PlacedBlock, needed: Long): Boolean; fun available(consumer: PlacedBlock): Long }` — aplica `EnergyRules.plan`, debita `energia` de cada fonte e `markDirty`; grava `sem_energia` 0/1 no consumidor.
- `object MachineMenus { fun canUse(player, placed): Boolean; fun energyLine(stored: Long, capacity: Long): String }`

- [ ] `./gradlew :plugins:oceanblock:system:machines:compileKotlin` com `localDeploy`-equivalente: rodar `./gradlew :plugins:oceanblock:plugin:localJar` — compila.
- [ ] Commit `feat(machines): gerador de energia, painel solar e energia por raio`.

### Task 8: Fornalha elétrica (Bukkit)

**Files:** `furnace/FurnaceRecipes.kt` (`fun load(): Map<String, String>` via `Bukkit.recipeIterator()`, só `FurnaceRecipe` com `RecipeChoice.MaterialChoice`), `furnace/FurnaceBlock.kt`, `furnace/FurnaceMenu.kt`.

- Menu 3 linhas: entradas colunas 2,4,6,8 da linha 1, progresso abaixo, saídas na linha 3, status de energia na coluna 1 linha 2. Clique no inventário de baixo coloca (`FurnaceRules.insert`, item sem id custom), clique na entrada devolve, na saída coleta; `saveNow` em retiradas.

- [ ] `localJar` compila. Commit `feat(machines): fornalha elétrica`.

### Task 9: Minerador (Bukkit)

**Files:** `miner/MinerBlock.kt`, `miner/MinerMenu.kt`.

- Tick: `advance`; se `isDue` e bloco abaixo `canMine`, mesma ilha, não custom, drops (`getDrops(ItemStack(IRON_PICKAXE))`) cabem no armazenamento após tentar esvaziar no contêiner encostado (acima + 4 laterais, `state is Container`, não custom) e `EnergyNetwork.draw(placed, 40)`: efeito `Effect.STEP_SOUND`, `block.type = AIR`, entrega, progresso 0.
- Menu 4 linhas: 27 slots guardados (clique retira), linha 4 com status e "Coletar tudo".

- [ ] `localJar` compila. Commit `feat(machines): minerador`.

### Task 10: Picareta, receitas, init e wiring

**Files:** `pickaxe/SmelterPickaxeItem.kt` (+listener `BlockDropItemEvent`), `recipe/MachineRecipes.kt`, `MachinesSystem.kt`, `OceanBlockPlugin.kt` (init após `StationsSystem`, `onDisable` não precisa — estado está no framework).

- Receitas da tabela da spec §6, `research = id`; Fundição/Fundição rápida via `StationIngredients.custom`.
- `MachinesSystem.init(plugin, viewFrame)`: registra 4 blocos, item, receitas, carrega `FurnaceRecipes`, listener de place/break de fonte que chama `EnergyIndex.rebuild()` no próximo tick, task de 100 ticks para `rebuild`.

- [ ] Todos os testes dos módulos tocados: `./gradlew :plugins:oceanblock:system:machines:test :plugins:oceanblock:system:craft:test :plugins:oceanblock:system:research:localTest :plugins:textura:localTest` PASS.
- [ ] Commit `feat(machines): picareta fundidora, receitas e registro do sistema`.

### Task 11: Instalar, verificar, documentar

- [ ] Pedir à sessão principal para parar o servidor; `./gradlew :plugins:oceanblock:plugin:localDeploy :plugins:textura:localDeploy`.
- [ ] Abrir aba nova com `.\start.bat`; log com "Done (" e "Hash:" sem erros novos; `data/block-allocations.yml` recebeu os 4 ids.
- [ ] CLAUDE.md: seção *Machines (`system/machines`)*, lista de módulos com testes, categorias 4. Manual de aceite: nova seção de máquinas.
- [ ] Commit `docs(machines): CLAUDE.md e manual de aceite das máquinas` (incluindo `block-allocations.yml` se rastreado).
