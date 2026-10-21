# Peneira, Fundição e Mesa de Pesquisa — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Primeira fonte de minério (peneira → pedaços → fundição → lingote) e a Mesa de Pesquisa que libera níveis melhores, automação e a Rede de Detritos por time do raft.

**Architecture:** Dois módulos novos sobre o framework de `custom-items`: `system/stations` (peneira, fundição, pedaços, `peneira.yml`) e `system/research` (definições, estado por raft em Mongo, timer, Mesa e dialog). O `craft` ganha um campo de pesquisa por receita e uma porta de bloqueio que o `research` preenche no `init`, sem o `craft` depender dele. Toda regra de jogo é uma função pura com Kotest; a parte Bukkit só traduz eventos.

**Tech Stack:** Kotlin 2.3 / JDK 25, Paper 26.3, Kotest 5.9, Mongo (kotlin sync driver via `ocean_block.libs.connection.Mongo`), Textura `DialogApi` / `NotificationApi`.

**Spec:** `docs/superpowers/specs/2026-10-21-oceanblock-peneira-fundicao-pesquisa-design.md`

## Global Constraints

- Nenhuma receita nova é vanilla: tudo passa pelo `/receitas` (`Recipes.register`).
- Ids de blocos/itens/pesquisas exatamente como no spec: `peneira`, `peneira_ferro`, `peneira_diamante`, `peneira_automatica`, `fundicao`, `fundicao_melhorada`, `fundicao_funil`, `mesa_pesquisa`, `pedaco_ferro`, `pedaco_cobre`, `pedaco_ouro`; pesquisas `rede`, `peneira_ferro`, `fundicao_melhorada`, `bancada_auxiliar`, `peneira_diamante`, `peneira_automatica`, `fundicao_funil`, `bancada_auxiliar_2`.
- Nenhuma pesquisa da v1 passa de 5 minutos; vagas começam em 1.
- Pedaço só vira lingote na Fundição (4 → 1); minério bruto vanilla entra 1 → 1.
- Quem usa peneira/fundição/Mesa: `CollectorRule.canCollect(island, player)` (dono, membros, confiados).
- Textos para o jogador em pt-BR; separador de custo `∙` (U+2219), nunca `·`.
- Systems nunca dependem do módulo `plugin`; dependências entre systems são `compileOnly`.
- Todo manager que bufferiza escrita entra em `OceanBlockPlugin.onDisable()` no mesmo commit.
- `Material`/`ItemStack` não existem em teste JVM puro no Paper 26.3: regras recebem ids `String`, nunca `Material` ou `ItemStack`.

## Review Focus

- Clique repetido muito rápido (autoclick) ou dois jogadores na mesma peneira: no máximo uma etapa a cada 4 ticks por jogador, e o estado é do bloco, não do jogador — teste `SieveProgressTest` "duas etapas no mesmo tick contam uma" (Task 5).
- Quebrar peneira carregada ou fundição com entrada/combustível/saída: o item do bloco carrega os contadores e a storage, e ao recolocar volta igual — teste de ida e volta dos contadores no `StateCodecTest` (Task 1) e de `SmelterState` ↔ contadores (Task 6).
- Restart ou raft offline no meio de uma pesquisa: o `startedAt` é relógio de parede, e ao carregar a pesquisa vencida conclui sem esperar de novo — teste `ResearchRulesTest` "conclui o que venceu enquanto offline" (Task 8).
- Entrega com mais itens do que falta, ou com pilhas parciais de madeiras diferentes: só o que falta sai do inventário — teste `ResearchRulesTest` "entrega limita ao que falta" (Task 8).
- Jogador sem raft (visitante, recém-chegado) abre receitas: receita com pesquisa aparece bloqueada, nunca liberada por engano — teste `ResearchLocksTest` "sem raft fica bloqueada" (Task 9).

---

### Task 1: Contadores persistidos no `PlacedBlock` e leitura de id custom

**Files:**
- Modify: `plugins/oceanblock/system/custom-items/src/main/kotlin/ocean_block/system/custom_items/block/PlacedBlock.kt`
- Modify: `.../custom_items/block/StateCodec.kt`, `.../custom_items/block/CustomBlockKeys.kt`, `.../custom_items/block/PlacedBlockCodec.kt`, `.../custom_items/block/PlacedBlockRepository.kt` (migração de campo ausente, se houver `migrateMissingFields`; senão o default do construtor basta)
- Create: `.../custom_items/CustomIds.kt`
- Test: `plugins/oceanblock/system/custom-items/src/test/kotlin/ocean_block/system/custom_items/block/StateCodecTest.kt`

**Interfaces:**
- Produces: `PlacedBlock.counters: MutableMap<String, Long>` (default vazio); `StateCodec.encodeCounters(Map<String, Long>): String` / `decodeCounters(String): MutableMap<String, Long>`; `CustomBlockKeys.COUNTERS`; `CustomIds.of(stack: ItemStack): String?` (lê `CustomItemKeys.ID`, senão `CustomBlockKeys.ID`).

- [ ] **Step 1: Teste** — em `StateCodecTest`: `encodeCounters(mapOf("fuel" to 300L, "progress" to 0L))` decodificado devolve o mesmo mapa; `decodeCounters("")` é vazio; entrada malformada (`"a=x;b=2"`) ignora só o par ruim e mantém `b=2`.
- [ ] **Step 2:** Rodar `./gradlew :plugins:oceanblock:system:custom-items:test` — FAIL (função inexistente).
- [ ] **Step 3:** Implementar no mesmo estilo de `encodeUpgrades`; gravar/ler `COUNTERS` no `PlacedBlockCodec` só quando não vazio; adicionar o campo ao `PlacedBlock` (último parâmetro, default `mutableMapOf()`).
- [ ] **Step 4:** Rodar de novo — PASS.
- [ ] **Step 5:** Commit `feat(custom-items): persist numeric counters on placed blocks`.

### Task 2: Receitas com pesquisa no `craft`

**Files:**
- Modify: `plugins/oceanblock/system/craft/src/main/kotlin/ocean_block/system/craft/recipe/OceanRecipe.kt`, `RecipeCategory.kt`, `RecipeCrafter.kt`, `CraftResult.kt`
- Create: `.../craft/recipe/RecipeLocks.kt`
- Modify: `.../craft/screen/RecipeEntries.kt`, `RecipesScreen.kt`
- Test: `plugins/oceanblock/system/craft/src/test/kotlin/ocean_block/system/craft/screen/RecipeEntriesTest.kt`

**Interfaces:**
- Produces: `OceanRecipe.research: String? = null` (último parâmetro); `RecipeCategory.PROCESSAMENTO(displayName = "Processamento", icon = "ui:sparkle", description = "Estações que transformam o que o mar traz.")`; `object RecipeLocks { fun install(provider: (Player, OceanRecipe) -> String?); fun lockedBy(player: Player, recipe: OceanRecipe): String? }` (provider devolve o **nome** da pesquisa que falta, `null` = liberada; sem provider instalado, tudo liberado); `CraftResult.Locked(research: String)`; `CardInput.lockedBy: String? = null`.

- [ ] **Step 1: Teste** — `card` com `lockedBy = "Peneira de ferro"` devolve `falta = "true"` e `acao = "⌀ Pesquise na Mesa: Peneira de ferro"`, mesmo sem shortages; sem `lockedBy` o comportamento atual não muda. Teste de enum: `RecipeCategory.entries.size <= RecipeEntries.CATEGORIES_PER_PAGE` continua passando.
- [ ] **Step 2:** `./gradlew :plugins:oceanblock:system:craft:test` — FAIL.
- [ ] **Step 3:** Implementar. `RecipeCrafter.craft` checa `RecipeLocks.lockedBy` antes de contar o inventário e devolve `Locked`; `RecipesScreen` preenche `lockedBy` no `cardInput` e responde `Locked` com `<red>Pesquise na Mesa: <nome>`.
- [ ] **Step 4:** Rodar — PASS.
- [ ] **Step 5:** Commit `feat(craft): research-gated recipes and processing category`.

### Task 3: Deriva traz areia, cascalho e terra; Rede passa a ser pesquisa

**Files:**
- Modify: `plugins/oceanblock/system/drift/src/main/resources/drift.yml` (bundled) e `server/plugins/ocean-block/drift.yml` (cópia de dev, mesmo conteúdo novo)
- Modify: `.../drift/recipe/DriftRecipes.kt` (`rede()` com `research = "rede"`)
- Modify: `.../drift/collect/CollectNotifier.kt`
- Test: `plugins/oceanblock/system/drift/src/test/kotlin/ocean_block/system/drift/config/DriftConfigParserTest.kt`

**Interfaces:**
- Produces: `CollectNotifier.notify(player: Player, key: String, label: String, icon: String, amount: Int)` (mesma tally e região `coleta`; a versão por `Material` passa a delegar para esta com `key = material.name`, `label = "<lang:${material.translationKey()}>"`, `icon = iconOf(material)`).
- Valores: tipos `torrao_areia` (`SAND` 1–3, chance 1.0), `torrao_cascalho` (`GRAVEL` 1–3, 1.0), `torrao_lodo` (`DIRT` 1–2, 1.0), `exibicao` = o próprio bloco, `escala` 0.5, pesos que somem ~25% do peso total atual; `barril` ganha `SAND` 2–4 chance 0.3 e `GRAVEL` 2–4 chance 0.3.

- [ ] **Step 1: Teste** — o parser, lendo o `drift.yml` bundled, devolve os três tipos novos com os drops acima.
- [ ] **Step 2:** `./gradlew :plugins:oceanblock:system:drift:test` — FAIL.
- [ ] **Step 3:** Editar os yml, o `DriftRecipes` e o `CollectNotifier`.
- [ ] **Step 4:** Rodar — PASS.
- [ ] **Step 5:** Commit `feat(drift): sand, gravel and dirt clumps; net unlocked by research`.

### Task 4: Módulo `stations`: pedaços e tabelas da peneira

**Files:**
- Create: `plugins/oceanblock/system/stations/build.gradle.kts` (igual ao do drift + `compileOnly(projects.plugins.oceanblock.system.drift)`); incluir `":plugins:oceanblock:system:stations"` no `settings.gradle.kts`
- Create: `.../system/stations/StationsSystem.kt` (só registra itens nesta task)
- Create: `.../stations/item/OrePiece.kt`, `.../stations/sieve/SieveTier.kt`, `.../stations/sieve/SieveInput.kt`, `.../stations/sieve/SieveTable.kt`, `.../stations/sieve/SieveTableParser.kt`, `.../stations/sieve/SieveRoller.kt`, `.../stations/config/StationsFile.kt`
- Create: `plugins/oceanblock/system/stations/src/main/resources/peneira.yml`
- Test: `.../stations/src/test/kotlin/ocean_block/system/stations/sieve/SieveTableParserTest.kt`, `SieveRollerTest.kt`

**Interfaces:**
- Produces:
  - `class OrePiece(id: String, base: Material, displayName: String, metal: String) : CustomItem` com instâncias `OrePiece.IRON/COPPER/GOLD` (`pedaco_ferro`/`RAW_IRON`/`"Pedaço de ferro"`/metal `IRON_INGOT`, idem cobre e ouro); `OrePiece.byId(id: String): OrePiece?`.
  - `enum class SieveTier(val blockId: String, val steps: Int, val table: String)` = `WOOD("peneira", 6, "madeira")`, `IRON("peneira_ferro", 5, "ferro")`, `DIAMOND("peneira_diamante", 4, "diamante")`, `AUTOMATIC("peneira_automatica", 5, "ferro")`.
  - `enum class SieveInput(val material: String, val key: String)` = `SAND("SAND","areia")`, `GRAVEL("GRAVEL","cascalho")`, `DIRT("DIRT","terra")`; `SieveInput.of(material: String): SieveInput?`.
  - `data class SieveDrop(val output: String, val min: Int, val max: Int, val chance: Double)` — `output` é id de `OrePiece` ou nome de `Material`.
  - `class SieveTable(drops: Map<SieveInput, Map<String, List<SieveDrop>>>) { fun drops(input: SieveInput, tier: SieveTier): List<SieveDrop> }`.
  - `object SieveTableParser { fun parse(yaml: YamlConfiguration, isOutput: (String) -> Boolean, warn: (String) -> Unit): SieveTable }` — formato `areia.madeira: [ {saida: pedaco_ouro, min: 1, max: 1, chance: 0.12}, ... ]`.
  - `object SieveRoller { fun roll(drops: List<SieveDrop>, random: Random): List<Pair<String, Int>> }` — rolagem independente; nenhum acerto → lista vazia.
- Valores do `peneira.yml`: a tabela 4.4 do spec, linha por linha (percentuais como fração).

- [ ] **Step 1: Testes** — parser: arquivo com uma linha válida e uma com `saida: NAO_EXISTE` → 1 drop e 1 aviso; chance `1.5` → aviso e linha ignorada; `min > max` → aviso e ignora. Roller com `Random(1)` e chances `0.0`/`1.0` → só o de 1.0 sai, quantidade dentro de min..max; todas `0.0` → vazio. Parser do `peneira.yml` bundled (lido do classpath) → `drops(GRAVEL, IRON)` contém `pedaco_ferro` 1–2 a 0.40 e `REDSTONE` a 0.04.
- [ ] **Step 2:** `./gradlew :plugins:oceanblock:system:stations:test` — FAIL.
- [ ] **Step 3:** Implementar. `StationsFile.load(plugin)` copia o `peneira.yml` bundled para `server/plugins/ocean-block/peneira.yml` se ausente (mesmo jeito do `DriftFile`) e chama o parser com `isOutput = { OrePiece.byId(it) != null || Material.matchMaterial(it) != null }`. `StationsSystem.init(plugin)` registra os três `OrePiece` no `CustomItemRegistry`; adicionar a chamada em `OceanBlockPlugin.setupSystems()` logo depois do `DriftSystem`.
- [ ] **Step 4:** Rodar — PASS; `./gradlew :plugins:oceanblock:plugin:shadowJar` compila.
- [ ] **Step 5:** Commit `feat(stations): ore pieces and sieve loot tables`.

### Task 5: Peneira (blocos, uso, automática, receitas)

**Files:**
- Create: `.../stations/sieve/SieveProgress.kt`, `.../stations/sieve/SieveBlock.kt`, `.../stations/sieve/SieveListener.kt`, `.../stations/sieve/SieveDelivery.kt`, `.../stations/recipe/StationRecipes.kt`
- Modify: `.../stations/StationsSystem.kt`
- Test: `.../stations/sieve/SieveProgressTest.kt`

**Interfaces:**
- Consumes: Task 1 `counters`, `CustomIds.of`; Task 2 `research`, `PROCESSAMENTO`; Task 3 `CollectNotifier.notify(player, key, label, icon, amount)`; Task 4 `SieveTier`, `SieveInput`, `SieveTable`, `SieveRoller`, `OrePiece`.
- Produces:
  - `data class SieveState(val input: SieveInput?, val stepsLeft: Int)`; `object SieveProgress { fun load(state: SieveState, input: SieveInput, tier: SieveTier): SieveState?` (null se já carregada); `fun step(state: SieveState): Step` com `sealed interface Step { data object Empty; data class Advanced(val state: SieveState); data class Done(val input: SieveInput) }`; `fun composterLevel(state: SieveState, tier: SieveTier): Int` (vazia 0, carregada 7, desce proporcional até 1); `fun allowed(lastTick: Long?, now: Long): Boolean` (true se `lastTick == null || now - lastTick >= 4`); `fun toCounters(state): Map<String, Long>` / `fromCounters(Map<String, Long>): SieveState` (chaves `sieve_input` = ordinal+1, `sieve_steps`).
  - `class SieveBlock(val tier: SieveTier) : CustomBlock` (`material = COMPOSTER`, `limitPerIsland = 6`); `SieveBlock.all: List<SieveBlock>`.
  - `object SieveDelivery { fun deliver(player: Player, outputs: List<Pair<String, Int>>) }` — monta `ItemStack` (pedaço via `OrePiece.build`, senão `Material`), inventário primeiro, sobra aos pés, notificação por `CollectNotifier.notify` (pedaço: `label` = nome do pedaço, `icon` = `item:raw_iron` etc.).
- Comportamento fixo: clique direito com SAND/GRAVEL/DIRT numa peneira vazia consome 1 e carrega; qualquer outro clique direito numa carregada dá uma etapa (partícula de bloco do input, som `BLOCK_SAND_HIT`/`BLOCK_GRAVEL_HIT`/`BLOCK_ROOTED_DIRT_HIT`, compostor no `composterLevel`); `Done` rola `table.drops(input, tier)` e entrega. `SieveListener` (prioridade `LOW`, antes do `CustomBlockListener`) nega sempre a interação vanilla do compostor e trata **shift+clique** (o framework ignora shift): carrega e já dá a primeira etapa. Cooldown por jogador em memória (`Map<UUID, Long>` de tick, limpo no quit).
- Quebrar uma peneira carregada: `onBreak` zera `sieve_input`/`sieve_steps` antes do item ser montado (perde só o material da etapa atual, como no spec).
- Automática: `tickInterval = 20`; se vazia, puxa 1 SAND/GRAVEL/DIRT do contêiner diretamente acima; dá uma etapa; em `Done` põe as saídas no contêiner diretamente abaixo — sem contêiner ou sem espaço, não dá a etapa final (fica esperando).
- Receitas (todas `PROCESSAMENTO`): `peneira` 4 Tábuas + 2 Linhas + 2 Gravetos; `peneira_ferro` 1 Peneira + 4 Lingotes de ferro (`research = "peneira_ferro"`); `peneira_diamante` 1 Peneira de ferro + 2 Diamantes + 4 Lingotes de ouro (`"peneira_diamante"`); `peneira_automatica` 1 Peneira de ferro + 4 Lingotes de ferro + 4 Redstone + 1 Funil (`"peneira_automatica"`). Ingrediente de bloco custom: `Ingredient(label, amount, plural) { CustomIds.of(it) == id }`.

- [ ] **Step 1: Testes** — `SieveProgressTest`: `load` numa vazia com WOOD → 6 etapas; `load` numa carregada → null; 6 `step` seguidos → 5 `Advanced` e 1 `Done(GRAVEL)`; `step` vazia → `Empty`; `composterLevel` vazia 0, recém-carregada 7, na última etapa ≥ 1; `allowed(10, 12)` false, `allowed(10, 14)` true, `allowed(null, 0)` true ("duas etapas no mesmo tick contam uma"); ida e volta `toCounters`/`fromCounters`, e mapa vazio → vazia.
- [ ] **Step 2:** Rodar os testes do módulo — FAIL.
- [ ] **Step 3:** Implementar e registrar no `StationsSystem.init` (blocos no `CustomBlockRegistry`, listener, receitas, `NotificationApi.registerItemIcons` de todas as saídas do `peneira.yml` + `raw_iron/raw_copper/raw_gold`).
- [ ] **Step 4:** Rodar — PASS; jar compila.
- [ ] **Step 5:** Commit `feat(stations): sieve with tiers and automatic variant`.

### Task 6: Regras da Fundição (puras)

**Files:**
- Create: `.../stations/smelter/SmelterTier.kt`, `.../stations/smelter/SmelterState.kt`, `.../stations/smelter/SmelterRules.kt`, `.../stations/smelter/Fuel.kt`
- Test: `.../stations/smelter/SmelterRulesTest.kt`

**Interfaces:**
- Produces:
  - `enum class SmelterTier(val blockId: String, val ticksPerIngot: Int, val bonusChance: Double, val hoppers: Boolean)` = `BASIC("fundicao", 200, 0.0, false)`, `IMPROVED("fundicao_melhorada", 100, 0.25, false)`, `HOPPER("fundicao_funil", 100, 0.25, true)`.
  - `object Fuel { fun ticks(material: String): Int? }` — planks 300, logs 1200, `STICK` 100, `COAL`/`CHARCOAL` 1600, `DRIED_KELP_BLOCK` 4000 (nome de material; tábuas e troncos por sufixo `_PLANKS`/`_LOG`/`_WOOD`).
  - `data class SmelterState(val input: String?, val inputCount: Int, val fuelItem: String?, val fuelCount: Int, val burnTicks: Int, val progressTicks: Int, val output: String?, val outputCount: Int)` com `SmelterState.EMPTY`; `toCounters()`/`fromCounters(map, ids: (Long) -> String?)` (strings guardadas como índice estável de uma lista fixa de ids: pedaços, `RAW_IRON`, `RAW_COPPER`, `RAW_GOLD`, metais e combustíveis).
  - `object SmelterRules`:
    - `fun insertOre(state, id: String, amount: Int): Pair<SmelterState, Int>` (devolve quantos entraram; máximo 32 na entrada; tipo diferente com entrada ocupada → 0);
    - `fun insertFuel(state, material: String, amount: Int): Pair<SmelterState, Int>` (máximo 16 guardados, um tipo por vez);
    - `fun ratio(id: String): Int` (pedaço 4, bruto vanilla 1);
    - `fun ingotOf(id: String): String` (pedaço → `IRON_INGOT`/`COPPER_INGOT`/`GOLD_INGOT`, bruto idem);
    - `fun tick(state, tier, ticks: Int, random: Random): SmelterState` — acende combustível guardado quando `burnTicks == 0` e há entrada suficiente; avança `progressTicks`; ao chegar em `ticksPerIngot` consome `ratio` da entrada e adiciona 1 lingote (+1 com `bonusChance`); para com saída em 16 ou com saída de outro metal;
    - `fun takeOutput(state): Pair<SmelterState, Pair<String, Int>?>`; `fun takeAll(state): Pair<SmelterState, List<Pair<String, Int>>>`.

- [ ] **Step 1: Testes** — 8 pedaços de ferro + 1 tábua na BASIC: depois de 200 ticks sai 1 `IRON_INGOT`, entrada 4, burn restante 100; depois de mais 100 ticks o combustível acaba e o progresso para sem nova tábua; `insertOre` de cobre com ferro na entrada → 0; 40 pedaços → entram 32; `RAW_COPPER` vira `COPPER_INGOT` 1 → 1; IMPROVED com `Random` que sempre acerta → 2 lingotes por ciclo em 100 ticks; saída cheia (16) não avança; ida e volta `toCounters`/`fromCounters` igual ao original.
- [ ] **Step 2:** Rodar — FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** Rodar — PASS.
- [ ] **Step 5:** Commit `feat(stations): smelter rules`.

### Task 7: Fundição no mundo

**Files:**
- Create: `.../stations/smelter/SmelterBlock.kt`, `.../stations/smelter/SmelterListener.kt`, `.../stations/smelter/SmelterDisplay.kt`, `.../stations/item/VanillaGuard.kt`
- Modify: `.../stations/recipe/StationRecipes.kt`, `.../stations/StationsSystem.kt`

**Interfaces:**
- Consumes: Task 6 inteira; Task 1 `counters`, `CustomIds.of`.
- Produces: `class SmelterBlock(val tier: SmelterTier) : CustomBlock` (`material = BLAST_FURNACE`, `limitPerIsland = 4`, `tickInterval = 5`); `SmelterBlock.all`.
- Comportamento fixo: clique direito com pedaço/bruto → `insertOre` com a pilha da mão; com combustível → `insertFuel`; mão vazia → `takeOutput` para o inventário (sobra aos pés). Tipo recusado → `<red>A fundição já está com outro material.` `SmelterListener` (`LOW`) nega a interface vanilla do alto-forno e trata **shift+mão vazia** = `takeAll`. Cada tick aplica `SmelterRules.tick(…, 5, random)`, grava `counters`, `markDirty`, liga/desliga `lit` do `BlockData` quando `burnTicks > 0` muda, e o `SmelterDisplay` mantém um `ItemDisplay` não persistente 0.6 bloco acima mostrando o material de entrada (removido quando vazia, no break e no `onDisable`). Funil (tier `HOPPER`): puxa do contêiner acima (minério) e do lado (combustível), empurra lingotes para baixo.
- `VanillaGuard` (listener): cancela `FurnaceStartSmeltEvent`/`FurnaceSmeltEvent`/`BlockCookEvent` cuja fonte tenha `CustomIds.of` não nulo, e zera o resultado de `PrepareItemCraftEvent` com qualquer ingrediente de id custom de `OrePiece`.
- Quebrar: o `CustomBlock.buildItem(placed)` já leva `storage` e (Task 1) `counters`; o display some.
- Receitas: `fundicao` 8 Argila (`CLAY_BALL`) + 4 Tábuas; `fundicao_melhorada` 1 Fundição + 6 Lingotes de ferro + 4 Lingotes de cobre (`"fundicao_melhorada"`); `fundicao_funil` 1 Fundição melhorada + 2 Funis + 2 Redstone (`"fundicao_funil"`).

- [ ] **Step 1:** Implementar e registrar.
- [ ] **Step 2:** `./gradlew :plugins:oceanblock:system:stations:test :plugins:oceanblock:plugin:shadowJar` — verde.
- [ ] **Step 3:** Commit `feat(stations): smelter blocks, display and vanilla guard`.

### Task 8: Modelo e regras de pesquisa (puras)

**Files:**
- Create: `plugins/oceanblock/system/research/build.gradle.kts` (deps `compileOnly`: textura, coordinate, formatter, instance, itemBuilder, connection, craft, customItems, islands, drift) e entrada no `settings.gradle.kts`
- Create: `.../system/research/model/ResearchDefinition.kt`, `.../model/ResearchCost.kt`, `.../model/IslandResearch.kt`, `.../model/ResearchStatus.kt`, `.../rules/ResearchRules.kt`, `.../tree/ResearchTree.kt`
- Test: `.../research/src/test/kotlin/ocean_block/system/research/rules/ResearchRulesTest.kt`, `.../tree/ResearchTreeTest.kt`

**Interfaces:**
- Produces:
  - `data class ResearchCost(val key: String, val label: String, val plural: String, val amount: Int)` (sem `ItemStack`; o casamento com o inventário fica na Task 9 por `key`).
  - `enum class ResearchCategory(val displayName: String, val icon: String)` = `COLETA("Coleta","ui:chest")`, `FUNDICAO("Fundição","ui:pickaxe")`, `RAFT("Raft","ui:sparkle")`.
  - `data class ResearchDefinition(val id: String, val name: String, val description: String, val category: ResearchCategory, val icon: String, val cost: List<ResearchCost>, val durationSeconds: Int, val requires: List<String>, val unlocksRecipes: List<String>, val extraSlots: Int = 0)`.
  - `data class RunningResearch(var id: String = "", var startedAt: Long = 0, var durationMs: Long = 0)` e `data class IslandResearch(var islandUuid: UUID, val delivered: MutableMap<String, MutableMap<String, Int>> = mutableMapOf(), val completed: MutableSet<String> = mutableSetOf(), val queue: MutableList<String> = mutableListOf(), val running: MutableList<RunningResearch> = mutableListOf())` (POJO Mongo: construtor com defaults).
  - `sealed interface ResearchStatus { data class Locked(val missing: List<String>); data object Available; data object Queued; data class Running(val remainingMs: Long); data object Done }`.
  - `object ResearchRules`:
    - `fun slots(state, tree: Map<String, ResearchDefinition>): Int` = 1 + soma de `extraSlots` das concluídas;
    - `fun status(def, state, now: Long): ResearchStatus`;
    - `fun missing(def, state): Map<String, Int>` (key → quanto falta);
    - `fun deliver(def, state, available: Map<String, Int>, now: Long, tree): Map<String, Int>` — grava em `delivered` até o que falta, devolve o que foi tirado; se completou, entra na fila e `startQueued`;
    - `fun startQueued(state, now, tree)` — enche vagas livres na ordem da fila;
    - `fun completeDue(state, now, tree): List<String>` — move vencidas para `completed` (limpa `delivered` delas), chama `startQueued` com `startedAt = startedAt + duração` de quem venceu (encadeia offline), devolve ids concluídos.
  - `object ResearchTree { val all: List<ResearchDefinition>; val byId: Map<String, ResearchDefinition> }` com a tabela 7.4 do spec, custos por `key`: `planks` (Tábua/Tábuas), `string` (Linha/Linhas), `iron_ingot` (Lingote de ferro/Lingotes de ferro), `copper_ingot`, `gold_ingot`, `clay_ball` (Argila/Argilas), `diamond` (Diamante/Diamantes), `redstone` (Redstone/Redstone); `unlocksRecipes` = id da pesquisa para as que liberam receita; `bancada_auxiliar` e `bancada_auxiliar_2` com `extraSlots = 1`.

- [ ] **Step 1: Testes** — `ResearchTreeTest`: todo `requires` existe na árvore; nenhuma duração > 300 s; não há ciclo. `ResearchRulesTest`:
  - "entrega limita ao que falta": `rede` com `available = {planks: 25, string: 2}` tira 10 e 2, status continua `Available`;
  - segunda entrega `{string: 9}` tira 4, entra rodando com `startedAt = now`;
  - duas pesquisas completas com 1 vaga → uma `Running`, outra `Queued`;
  - `completeDue` antes do fim → vazio; depois → `["rede"]`, a da fila começa em `startedAt + 20 000`;
  - "conclui o que venceu enquanto offline": duas na fila de 1 vaga, `now` = início + 10 min → ambas concluídas na mesma chamada;
  - `bancada_auxiliar` concluída → `slots == 2`;
  - `status` de `peneira_diamante` sem `peneira_ferro` → `Locked(["peneira_ferro"])`.
- [ ] **Step 2:** `./gradlew :plugins:oceanblock:system:research:test` — FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** Rodar — PASS.
- [ ] **Step 5:** Commit `feat(research): research model, tree and rules`.

### Task 9: Persistência, timer, bloqueio de receitas e notificação

**Files:**
- Create: `.../research/data/ResearchRepository.kt`, `.../research/data/ResearchManager.kt`, `.../research/ResearchTimer.kt`, `.../research/ResearchLocks.kt`, `.../research/ResearchNotifier.kt`, `.../research/ResearchSystem.kt`
- Modify: `plugins/oceanblock/plugin/src/main/kotlin/io/github/badnotice/ocean_block/OceanBlockPlugin.kt` (`ResearchSystem.init(this, viewFrame)` depois de `StationsSystem`; `ResearchManager.onDisable()` no `onDisable`)
- Test: `.../research/ResearchLocksTest.kt`

**Interfaces:**
- Consumes: Task 8; Task 2 `RecipeLocks.install`, `OceanRecipe.research`.
- Produces:
  - `ResearchRepository` — coleção `island_research`, índice único em `islandUuid`, `toList()`, `updateAll(list)` (bulk upsert), `update(state)`.
  - `object ResearchManager { fun onInit(); fun onDisable(); fun of(islandUuid: UUID): IslandResearch` (cria vazio sob demanda); `fun markDirty(state); fun saveNow(state); fun running(): List<IslandResearch>` }` — flush dos sujos a cada 30 s, limpando o set só depois do `updateAll` dar certo.
  - `object ResearchLocks { fun reason(recipe: OceanRecipe, completed: Set<String>?): String? }` — `null` se `recipe.research == null` ou se está em `completed`; senão o `name` da pesquisa (ou o id se não existir na árvore); `completed == null` (sem raft) → bloqueada.
  - `ResearchNotifier.completed(island, def)` — `NotificationApi.push` na região `avisos` para `island.getIslandOnlinePlayers()`: `NotificationLine("<green>Pesquisa concluída:</green> <white>${def.name}</white>", def.icon item, 100, "pesquisa:${def.id}")` + som `UI_TOAST_CHALLENGE_COMPLETE`.
  - `ResearchTimer` — task de 20 ticks: para cada `ResearchManager.running()`, `completeDue(now = System.currentTimeMillis())`; se concluiu algo, `markDirty` e notifica. Ao abrir a Mesa também roda `completeDue` do raft antes de desenhar.
  - `ResearchSystem.init` registra em `NotificationApi.registerItemIcons` o material de `icon` de toda `ResearchDefinition` (durante o `onEnable`) e instala `RecipeLocks.install { player, recipe -> ResearchLocks.reason(recipe, islandOf(player)?.let { ResearchManager.of(it.uuid).completed }) }` (raft por `IslandManager.playerIsland[player.uniqueId]` → `islandsById`).

- [ ] **Step 1: Teste** — `ResearchLocksTest`: receita sem `research` → null; com `research = "rede"` e `completed = {"rede"}` → null; `completed = {}` → `"Rede de Detritos"`; "sem raft fica bloqueada": `completed = null` → `"Rede de Detritos"`.
- [ ] **Step 2:** Rodar — FAIL.
- [ ] **Step 3:** Implementar e ligar no plugin.
- [ ] **Step 4:** Testes do módulo PASS; `shadowJar` compila.
- [ ] **Step 5:** Commit `feat(research): persistence, timer, recipe locks and notifications`.

### Task 10: Mesa de Pesquisa e dialog

**Files:**
- Create: `.../research/table/ResearchTableBlock.kt`, `.../research/screen/ResearchEntries.kt`, `.../research/screen/ResearchScreen.kt`, `.../research/screen/ResearchView.kt`, `.../research/ResearchRecipes.kt`
- Create: `server/plugins/Textura/configs/ui/pesquisa.json` (fazer no Textura Studio, tema `stone`)
- Test: `.../research/screen/ResearchEntriesTest.kt`; `plugins/textura/src/test/kotlin/one_block/textura/dialog/PesquisaScreenTest.kt` (modelo: `ReceitasScreenTest`)

**Interfaces:**
- Consumes: Task 8 e 9.
- Produces:
  - `ResearchTableBlock : CustomBlock` (`id = "mesa_pesquisa"`, `material = CARTOGRAPHY_TABLE`, `limitPerIsland = 2`); clique direito → `ResearchScreen.open(player, island)`. Um listener `LOW` nega a interface vanilla da mesa de cartografia.
  - `object ResearchEntries` (puro):
    - `fun categories(active: ResearchCategory): List<Map<String,String>>` (chaves `nome`, `icone`, `ativa`, `acao`);
    - `fun card(def, state, now): Map<String,String>` com `icone`, `nome`, `descricao`, `custo_lista` (linhas `∙ 3/10 Tábuas`), `estado` (`Bloqueada`, `Disponível`, `Na fila`, `Pesquisando`, `Concluída`), `tempo` (`faltam 1m20s` / `leva 2min` / vazio), `pode_entregar` (`"true"` só em `Available` com algo faltando), `acao` (texto do botão: `Entregar`, `Na fila`, `faltam 1m20s`, `✔ Concluída`, `⌀ Requer: Peneira de ferro`);
    - `fun queue(state, now): List<Map<String,String>>` (`nome`, `estado`, `tempo`);
    - `fun slotsText(used: Int, total: Int): String` = `"Vagas: $used/$total"`;
    - `fun duration(ms: Long): String` — `45s`, `1m20s`, `5min`.
  - Dialog `pesquisa.json`: lista `categorias` (nav, perPage 3, bind `categoria`), `pesquisas` (grid 2 colunas, perPage 6, botão bind `entregar`, `disabled` = `{bloqueado}` onde `bloqueado` = inverso de `pode_entregar`), `fila` (perPage 3), textos `vagas` e `titulo`, botão `atualizar`.
  - `ResearchScreen`: `listButton("entregar")` conta o inventário por `key` (`planks` = `Tag.PLANKS`, demais por `Material`), chama `ResearchRules.deliver`, remove do inventário exatamente o devolvido, `ResearchManager.saveNow`, redesenha; `button("atualizar")` redesenha. Erros do `DialogApi.openResult` iguais ao `RecipesScreen`.
  - Receita livre `mesa_pesquisa`: 6 Tábuas + 3 Linhas + 2 Pepitas de ferro (`IRON_NUGGET`), categoria `PROCESSAMENTO`.

- [ ] **Step 1: Testes** — `ResearchEntriesTest`: card de `rede` sem entregas → `custo_lista` contém `∙ 0/10 Tábuas` e `∙ 0/6 Linhas`, `pode_entregar = "true"`; rodando com 80 s restantes → `acao = "faltam 1m20s"`; concluída → `"✔ Concluída"`; bloqueada → `"⌀ Requer: Peneira de ferro"`; `duration(45_000) == "45s"`, `duration(300_000) == "5min"`. `PesquisaScreenTest`: compila o `pesquisa.json` real com binds de exemplo sem avisos de chave desconhecida.
- [ ] **Step 2:** Rodar `:plugins:oceanblock:system:research:test` e `:plugins:textura:localTest` — FAIL.
- [ ] **Step 3:** Implementar; montar o `pesquisa.json` no Studio.
- [ ] **Step 4:** Rodar — PASS; `shadowJar` compila.
- [ ] **Step 5:** Commit `feat(research): research table and dialog`.

### Task 11: Documentação e aceite manual

**Files:**
- Modify: `CLAUDE.md` (seções novas `system/stations` e `system/research`, no estilo de `drift`/`craft`: blocos, ids, configs, regras puras testadas, limites de teste)
- Modify: `docs/superpowers/plans/2026-10-18-oceanblock-aceite-manual.md` (seção nova)

- [ ] **Step 1:** Escrever a seção de aceite manual: peneirar cascalho até sair pedaço (notificação `coleta`); shift+clique carrega e dá etapa; pedaço recusado na fornalha vanilla e na bancada; fundir 8 pedaços com tábua e retirar lingote; quebrar fundição carregada e recolocar (tudo volta); Rede bloqueada no `/receitas` de um raft novo, pesquisar do zero (entrega parcial por dois membros), notificação `avisos` ao concluir; duas pesquisas completas com 1 vaga (fila); `bancada_auxiliar` libera 2 vagas; desligar o servidor no meio de uma pesquisa, ligar depois do tempo e abrir a Mesa (já concluída); peneira automática entre baú de cima e funil de baixo.
- [ ] **Step 2:** Atualizar o `CLAUDE.md`.
- [ ] **Step 3:** `./gradlew build` verde.
- [ ] **Step 4:** Commit `docs: stations and research systems`.
