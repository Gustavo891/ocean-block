# Progressão: nível do jogador e missões — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Neste projeto: execução inline (superpowers:executing-plans), sem subagents.

**Goal:** Trocar a pesquisa por nível do jogador (1–50, XP de detritos, gerador, receitas e missões), com trilha de missões e diárias no HUD do topo e os diálogos `/nivel` e `/missoes` no tema Maré.

**Architecture:** Dois módulos novos, `system/level` e `system/missions`, no padrão `XxxSystem` + Manager/Repository. Os módulos existentes só ganham eventos (`drift`, `islands`) e o campo `OceanRecipe.level` (`craft`); `level` e `missions` ouvem esses eventos e nada passa a depender deles. Regras puras com Kotest; lado Bukkit no manual de aceite.

**Tech Stack:** Kotlin 2.3, Paper 26.3, Kotest 5.9, MongoDB (bson-kotlin), Aikar ACF, Textura (`HudApi`, `DialogApi`, `NotificationApi`).

**Spec:** `docs/superpowers/specs/2026-10-24-oceanblock-progressao-nivel-design.md`

## Global Constraints

- Sem comentários no código (nem KDoc, nem YAML gerado).
- Textos para o jogador em pt-BR.
- Commits `type(scope): descrição` terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, na `main`.
- Trabalho na thread principal, sem subagents.
- Gradle sempre por tarefa com `local` no nome quando o módulo depende de `lib:connection` (`localTest`); `craft`, `drift`, `islands` testam com `test`.
- `MAX_LEVEL = 50`; `toNext(n) = round(100 × n^1,5)`.
- Coleções novas: `player_levels`, `player_missions`. Apagar `island_research`.
- Diárias: 3 por dia, data em `America/Sao_Paulo`.
- Placeholders PlaceholderAPI precisam de `_`; PNG de glyph até 256 px; glyph com `ascent` > `height` é rejeitado.
- Nunca parar/reiniciar o servidor sem o sim do usuário.

## Review Focus

- Um `award` grande (admin `dar`) que cruza vários níveis: um título só com o nível final e a lista de todas as receitas abertas no intervalo → teste em `LevelUpsTest`.
- XP no nível 50: não passa de `totalFor(50)`, fração 1,0 → teste em `LevelCurveTest`.
- Trilha chega numa missão de estado já cumprida (jogador já nível 3 na missão 5, ilha já com upgrade na 9): conclui em cadeia na hora → teste em `MissionRulesTest`.
- Jogador sem ilha nas missões 2 e 9: não quebra, missão segue aberta → teste em `MissionRulesTest`.
- Virada de dia com o jogador online ou após dias offline: re-sorteia uma vez, descarta o progresso velho → teste em `MissionRulesTest`.

---

### Task 1: Receitas por nível e remoção da pesquisa

**Files:**
- Delete: `plugins/oceanblock/system/research/` (módulo inteiro)
- Modify: `settings.gradle.kts` (tirar `:plugins:oceanblock:system:research`)
- Modify: `plugins/oceanblock/plugin/src/main/kotlin/io/github/badnotice/ocean_block/OceanBlockPlugin.kt` (tirar `ResearchSystem.init`/`onDisable` e o import)
- Modify: `plugins/oceanblock/system/craft/src/main/kotlin/ocean_block/system/craft/recipe/{OceanRecipe,RecipeLocks,RecipeCrafter,CraftResult}.kt`
- Modify: `plugins/oceanblock/system/craft/src/main/kotlin/ocean_block/system/craft/menu/{RecipeLore,RecipesMenu}.kt`
- Modify: `plugins/oceanblock/system/drift/src/main/kotlin/ocean_block/system/drift/recipe/DriftRecipes.kt`, `.../stations/recipe/StationRecipes.kt`, `.../machines/recipe/MachineRecipes.kt`
- Modify: `plugins/oceanblock/system/islands/src/main/kotlin/ocean_block/system/islands/view/IslandView.kt` (botão "Pesquisa" → "Nível")
- Modify: `plugins/oceanblock/system/stations/src/test/kotlin/ocean_block/system/stations/item/StationBlockIdsTest.kt` (só se citar pesquisa de forma que quebre; `mesa_pesquisa` pode ficar)
- Test: `plugins/oceanblock/system/craft/src/test/kotlin/ocean_block/system/craft/menu/RecipeLoreTest.kt`, `.../recipe/RecipeLocksTest.kt` (novo)

**Interfaces:**
- Produces: `OceanRecipe.level: Int = 1`; `RecipeLocks.install(provider: (Player) -> Int)`; `RecipeLocks.requiredLevel(player: Player, recipe: OceanRecipe): Int?`; pura `RecipeLocks.required(recipeLevel: Int, playerLevel: Int?): Int?`; `CraftResult.Locked(level: Int)`; `RecipeCard.requiredLevel: Int?`.

- [ ] **Step 1: Testes que falham** — em `RecipeLoreTest`: card com `requiredLevel = 8` tem status `<red>Requer nível 8`; toda lore contém `<dark_gray>Nível N` logo após a descrição; `gancho.level shouldBe 1`. Em `RecipeLocksTest`: `required(1, null) shouldBe null` (sem provider), `required(8, 7) shouldBe 8`, `required(8, 8) shouldBe null`, `required(1, 1) shouldBe null`.
- [ ] **Step 2: Rodar** `./gradlew :plugins:oceanblock:system:craft:test` → FAIL (compilação).
- [ ] **Step 3: Implementar** — `research` sai do `OceanRecipe`, entra `level`. `RecipeLocks` guarda `provider: ((Player) -> Int)?` e `requiredLevel` usa `required(recipe.level, provider?.invoke(player))`. `RecipeCrafter.craft` devolve `Locked(level)`. `RecipesMenu`: mensagem `<red>Você precisa do nível <white>N</white> para craftar isso.` e card com `requiredLevel = RecipeLocks.requiredLevel(player, recipe)`. Níveis: `gancho` 1, `rede` 3, `fundicao` 1, `fundicao_melhorada` 8, `fundicao_funil` 15, `picareta_fundidora` 12, `gerador_energia` 18, `painel_solar` 20, `fornalha_eletrica` 22, `minerador` 25, brocas 28/32/36/40 (por `DrillTier`). `StationRecipes.smelter` e `MachineRecipes.recipe` trocam o parâmetro `research` por `level: Int`. Apagar o módulo research e suas referências; `IslandView` passa a `action(Material.EXPERIENCE_BOTTLE, "Nível", "Mostra seu nível e as receitas.")` rodando `nivel`.
- [ ] **Step 4: Rodar** `./gradlew :plugins:oceanblock:system:craft:test :plugins:oceanblock:system:stations:test :plugins:oceanblock:system:machines:test :plugins:oceanblock:system:drift:test` e `./gradlew :plugins:oceanblock:plugin:localJar` → PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `refactor(craft): receitas travadas por nível e remoção da pesquisa`.

### Task 2: Eventos de detritos e do gerador

**Files:**
- Create: `plugins/oceanblock/system/drift/src/main/kotlin/ocean_block/system/drift/collect/DebrisCollectedEvent.kt`
- Modify: `.../drift/debris/DebrisMotion.kt` (`deliver(debris, player, byHook: Boolean)`; `pullToPlayer` passa `true`, `touchCollect` passa `false`)
- Create: `plugins/oceanblock/system/islands/src/main/kotlin/ocean_block/system/islands/generator/{GeneratorBlocks,BlockKey,GeneratorBlockBrokenEvent,GeneratorFormedEvent,GeneratorBreakListener}.kt`
- Create: `plugins/oceanblock/system/islands/src/main/kotlin/ocean_block/system/islands/island/IslandUpgradeEvent.kt`
- Modify: `.../islands/generator/GeneratorListener.kt` (marca a posição e dispara `GeneratorFormedEvent`), `.../islands/view/UpgradeView.kt` (dispara `IslandUpgradeEvent` após a compra), registro do `GeneratorBreakListener` onde o `GeneratorListener` é registrado
- Test: `plugins/oceanblock/system/islands/src/test/kotlin/ocean_block/system/islands/generator/GeneratorBlocksTest.kt`

**Interfaces:**
- Produces: `DebrisCollectedEvent(player: Player, typeId: String, byHook: Boolean)`; `GeneratorBlockBrokenEvent(player: Player, material: Material)`; `GeneratorFormedEvent(island: Island)`; `IslandUpgradeEvent(island: Island, upgrade: IslandUpgrade, level: Int)`; `data class BlockKey(world: String, x: Int, y: Int, z: Int)`; `GeneratorBlocks.mark(key)`, `consume(key): Boolean`, `forget(key)`. Todos os eventos são `Event()` síncronos com `HandlerList` estático (mesmo formato do `CustomCraftEvent`).

- [ ] **Step 1: Teste que falha** — `GeneratorBlocksTest` (classe instanciável `GeneratorBlockSet`, o `object GeneratorBlocks` delega): `consume` de chave não marcada é `false`; `mark` + `consume` é `true` e um segundo `consume` é `false`; `mark` + `forget` + `consume` é `false`; chaves com mundos diferentes não colidem.
- [ ] **Step 2: Rodar** `./gradlew :plugins:oceanblock:system:islands:test --tests "*GeneratorBlocks*"` → FAIL.
- [ ] **Step 3: Implementar** — set concorrente (`ConcurrentHashMap.newKeySet`). `GeneratorBreakListener`: `BlockBreakEvent` MONITOR ignoreCancelled → `consume` → dispara `GeneratorBlockBrokenEvent(player, block.type)`; `BlockPlaceEvent` MONITOR ignoreCancelled → `forget`. Eventos de drift disparados com `Bukkit.getPluginManager().callEvent`.
- [ ] **Step 4: Rodar** `./gradlew :plugins:oceanblock:system:islands:test :plugins:oceanblock:system:drift:test` → PASS.
- [ ] **Step 5: Commit** `feat(islands): eventos de detrito coletado, bloco do gerador e upgrade`.

### Task 3: Núcleo do nível (`system/level`)

**Files:**
- Modify: `settings.gradle.kts` (adicionar `:plugins:oceanblock:system:level`)
- Create: `plugins/oceanblock/system/level/build.gradle.kts` (igual ao do research antigo: `paper-common`, `compileOnly` de `textura`, `mongodb`, `lib:connection`, `lib:formatter`, `lib:instance`, `craft`, `drift`, `islands`, `scoreboard`; testes com `paperApi`, kotest, `testImplementation(craft)`; `localTest`)
- Create em `src/main/kotlin/ocean_block/system/level/`: `LevelSystem.kt`, `curve/LevelCurve.kt`, `curve/LevelUps.kt`, `config/{LevelConfig,LevelConfigParser,LevelFile}.kt`, `data/{PlayerLevel,LevelRepository,LevelManager}.kt`, `PlayerLevels.kt`, `PlayerLevelUpEvent.kt`, `notify/{XpTally,LevelNotifier}.kt`, `listener/XpListener.kt`, `command/NivelCommand.kt` (só admin e fallback de chat nesta task)
- Create: `src/main/resources/nivel.yml` (valores da spec)
- Modify: `OceanBlockPlugin.kt` (`LevelSystem.init(this, viewFrame, paperCommandManager)` depois de `SellSystem` e `ScoreboardSystem`; `LevelSystem.onDisable()`)
- Test: `src/test/kotlin/ocean_block/system/level/{LevelCurveTest,LevelUpsTest,LevelConfigParserTest,XpTallyTest}.kt`

**Interfaces:**
- Consumes: `OceanRecipe.level`, `Recipes` (Task 1); eventos da Task 2; `CustomCraftEvent(player, id, amount)`; `AccountLevel.source`, `LevelProgress(level, progress)`.
- Produces: `LevelCurve.MAX_LEVEL`, `toNext(level: Int): Long`, `totalFor(level: Int): Long`, `progress(xp: Long): LevelProgress`, `levelOf(xp: Long): Int`; `LevelUps.unlocked(from: Int, to: Int, recipes: List<OceanRecipe>): List<OceanRecipe>`; `LevelConfig(debrisDefault: Int, debris: Map<String, Int>, generator: Map<String, Int>, recipeBase: Int, recipePerLevel: Int)` com `recipeXp(level: Int): Int`; `LevelConfigParser.parse(section: Map<String, Any?>, warn: (String) -> Unit): LevelConfig`; `LevelManager.xp(uuid: UUID): Long`, `setXp(uuid, xp)`; `PlayerLevels.award(player: Player, amount: Int)`, `PlayerLevels.levelOf(player: Player): Int`, `PlayerLevels.config: LevelConfig`; `PlayerLevelUpEvent(player, from: Int, to: Int)`; `XpTally(windowTicks: Long).add(amount: Int, tick: Long): Int`.

- [ ] **Step 1: Testes que falham**
  - `LevelCurveTest`: `toNext(1) shouldBe 100`, `toNext(10) shouldBe 3162`, `toNext(49) shouldBe 34300`; `totalFor(1) shouldBe 0`, `totalFor(2) shouldBe 100`; `progress(0) shouldBe LevelProgress(1, 0.0)`; `progress(150)` → nível 2, fração `50.0 / toNext(2)`; `progress(totalFor(50))` e `progress(totalFor(50) + 999_999)` → `LevelProgress(50, 1.0)`; `levelOf(-5) shouldBe 1`.
  - `LevelUpsTest`: com receitas de nível 1, 3, 8, 12: `unlocked(2, 3)` = [nível 3]; `unlocked(1, 12)` = [3, 8, 12] em ordem; `unlocked(5, 5)` vazio; `unlocked(12, 50)` vazio.
  - `LevelConfigParserTest`: o mapa da spec gera `debris["barril"] == 10`, `generator["DIAMOND_ORE"] == 20`, `recipeXp(8) == 34`; valor negativo ou texto vira o padrão e chama `warn` com `[nivel]`; seção vazia devolve os padrões da spec.
  - `XpTallyTest`: `add(5, 0)` = 5, `add(3, 10)` = 8 dentro da janela de 60, `add(4, 100)` = 4 depois da janela.
- [ ] **Step 2: Rodar** `./gradlew :plugins:oceanblock:system:level:localTest` → FAIL.
- [ ] **Step 3: Implementar**
  - `LevelCurve.toNext` = `Math.round(100.0 * n.toDouble().pow(1.5))`.
  - `PlayerLevel(uuid: String, xp: Long)`; `LevelRepository` (coleção `player_levels`, índice único `uuid`, `toList`, `updateAll` com `ReplaceOneModel` upsert); `LevelManager` copiado do formato do `ResearchManager` antigo (lock, `dirty`, flush 30 s, limpa só após sucesso, logs `[nivel]`). `setXp` grava `min(xp, totalFor(MAX_LEVEL))`.
  - `PlayerLevels.award`: sai cedo para `GameMode.CREATIVE`/`SPECTATOR` ou `amount <= 0`; calcula nível antes/depois; `LevelNotifier.xp(player, amount)`; se subiu, `LevelNotifier.levelUp(player, from, to, LevelUps.unlocked(from, to, Recipes.all()))` e dispara `PlayerLevelUpEvent`. Nada no nível 50.
  - `LevelNotifier`: textos e durações exatamente como na spec (região `coleta` chave `coleta:xp` 60 ticks; título `<aqua>NÍVEL N` / `<gray>Você subiu de nível!` fade 10/50/15; `ENTITY_PLAYER_LEVELUP`; região `avisos` chave `nivel` 100 ticks; `Liberado: A, B`; ícone da primeira receita via `CollectNotifier.iconOf`-equivalente `item:<material>` do resultado, senão `item:experience_bottle`).
  - `XpListener`: `DebrisCollectedEvent` → `config.debris[typeId] ?: debrisDefault`; `GeneratorBlockBrokenEvent` → `config.generator[material.name] ?: 0`; `CustomCraftEvent` → `recipeXp(Recipes.byId(id).level)`.
  - `LevelSystem.init`: `LevelFile.load` (copia `nivel.yml` se faltar), `LevelManager.onInit()`, `RecipeLocks.install { PlayerLevels.levelOf(it) }`, `AccountLevel.source = { LevelCurve.progress(LevelManager.xp(it.uniqueId)) }`, `NotificationApi.registerItemIcons(listOf("EXPERIENCE_BOTTLE") + ícones das receitas)`, registra listeners e `NivelCommand`, `ScheduledExecutor.execute { runCatching { Mongo.getCollection<Document>("island_research").drop() }.onFailure { log } }`.
  - `NivelCommand` (`@CommandAlias("nivel|level|niveis")`): `@Default` por enquanto escreve no chat `Nível N — X/Y XP` e a próxima receita; `@Subcommand("dar")` e `@Subcommand("definir")` com `@CommandPermission("oneblock.admin")` e `@CommandCompletion("@players")`.
- [ ] **Step 4: Rodar** `./gradlew :plugins:oceanblock:system:level:localTest` e `./gradlew :plugins:oceanblock:plugin:localJar` → PASS.
- [ ] **Step 5: Commit** `feat(level): nível do jogador com XP de detritos, gerador e receitas`.

### Task 4: Tema Maré e fundo do HUD

**Files:**
- Create: script descartável no scratchpad (Java2D ou Python) que gera os PNGs
- Create: `server/plugins/Textura/configs/ui/mare.yml`
- Create: `server/plugins/Textura/resource/assets/oneblock/textures/ui/mare/{sheet,head,well,slot,slot_missing,button,button_light,button_light_selected,button_light_disabled,chip,progress_track,progress_fill,switch_on,switch_off}.png` (+ `.png.slice` nas peças esticáveis, mesmo formato das de `maquina/`)
- Modify: arte dos glyphs `hud_left`/`hud_body`/`hud_right` (achar o PNG pelo `configs/glyphs/*.yml` que os declara)

**Interfaces:**
- Produces: tema `mare` com as mesmas chaves do `maquina.yml` (surfaces `sheet/head/well/frame`, `switch`, `button.looks.light`, `slots`, `chip`, `text` com `heading/body/muted/selected/button/count/segment/good/accent/bad/disabled/*-bare/tooltip`) e o par `progress` usado pelas telas (`ui:mare/progress_track`, `ui:mare/progress_fill`).

- [ ] **Step 1:** Ler `maquina.yml`, uma `.png.slice` e as dimensões dos PNGs de `maquina/` para copiar tamanhos e fatias.
- [ ] **Step 2:** Gerar a arte com a paleta da spec (sheet `#b89a72` com veio e borda `#7a5f3e`; head `#2f7f86` com corda tracejada `#d9c79a`; well `#d9c79a` borda `#9c7f58`; track `#1d4e63`; fill `#3fd0c9`; botão azul-petróleo com topo de espuma) e o fundo do HUD no mesmo estilo, sem passar de 256 px.
- [ ] **Step 3:** Escrever `mare.yml` (texto corpo `#3b2a17`, título claro, `good` verde-mar, `accent` turquesa escuro, `bad` coral; segmented/nav/dropdown/slider apontando para `ui:stone/*`).
- [ ] **Step 4:** Conferir no Studio (`http://127.0.0.1:7070`, aba Temas) que o tema carrega sem erro; renderizar um PNG de amostra e olhar.
- [ ] **Step 5: Commit** `feat(textura): tema Maré e fundo do HUD`.

### Task 5: Diálogo `/nivel`

**Files:**
- Create: `server/plugins/Textura/configs/ui/nivel.json` (tema `mare`)
- Create: `plugins/oceanblock/system/level/src/main/kotlin/ocean_block/system/level/dialog/{LevelDialog,LevelLore}.kt`
- Modify: `NivelCommand.kt` (`@Default` abre o diálogo; chat só como fallback)
- Test: `src/test/kotlin/ocean_block/system/level/LevelLoreTest.kt`

**Interfaces:**
- Consumes: `LevelCurve`, `LevelConfig`, `Recipes`, `PlayerLevels`.
- Produces: `LevelLore.header(xp: Long): Map<String, String>` (chaves `nivel`, `xp`, `prox`, `pct`, `barra`), `LevelLore.recipeEntry(recipe: OceanRecipe, playerLevel: Int): Map<String, String>` (chaves `item`, `nome`, `nivel`, `estado`, `estado_estilo`), `LevelLore.sources(config: LevelConfig): List<Map<String, String>>`; `LevelDialog.open(player): Boolean`, `LevelDialog.registerIcons()`.

- [ ] **Step 1: Teste que falha** — `LevelLoreTest`: `header(150)` → `nivel` "Nível 2", `xp` "50 / 283 XP", `pct` "17,67%"; `header(totalFor(50))` → `xp` "Nível máximo"; `recipeEntry` de nível 8 com jogador 7 → `estado` "✖ Bloqueada", `estado_estilo` "bad", `nivel` "Nível 8"; com jogador 8 → "✔ Liberada" / "good"; `sources` traz três linhas com "Detritos: 3 a 10 XP", "Gerador: 1 a 20 XP", "Receitas: 13 a 130 XP" para o `nivel.yml` padrão (receitas de nível 1 a 40).
- [ ] **Step 2: Rodar** `./gradlew :plugins:oceanblock:system:level:localTest --tests "*LevelLore*"` → FAIL.
- [ ] **Step 3: Implementar** `LevelLore` e a tela: card do nível (texto `{nivel}` como heading, `progress` com `value` da barra 0–100, `{xp}`, `{pct}`), card "Como ganhar XP" (lista `fontes`, ícones `item:fishing_rod`, `item:cobblestone`, `item:crafting_table`, mais a linha "Missões: veja /missoes"), card das receitas (lista `receitas` com `perPage` que caiba sem scroll e `nav` de página). Copiar a estrutura de nós de `fornalha.json`. `LevelDialog` usa `DialogApi.openResult`; `MissingScreen`/`Failed` → resumo no chat. `registerIcons` registra os materiais dos resultados das receitas (via `recipe.result().type`, chamado no `init`, antes do build do pack) e os três das fontes.
- [ ] **Step 4: Rodar** testes e `./gradlew :plugins:oceanblock:plugin:localJar` → PASS; prévia no Studio sem erro de parse.
- [ ] **Step 5: Commit** `feat(level): diálogo /nivel no tema Maré`.

### Task 6: Missões (`system/missions`)

**Files:**
- Modify: `settings.gradle.kts` (adicionar `:plugins:oceanblock:system:missions`)
- Create: `plugins/oceanblock/system/missions/build.gradle.kts` (como o de `level`, mais `compileOnly(level)` e `compileOnly(sell)`; `localTest`)
- Create em `src/main/kotlin/ocean_block/system/missions/`: `MissionsSystem.kt`, `model/{Mission,MissionObjective,MissionSignal,DailyMission,PlayerMissions}.kt`, `catalog/{MissionTrail,DailyPool}.kt`, `rules/MissionRules.kt`, `text/MissionText.kt`, `data/{MissionRepository,MissionManager}.kt`, `MissionTracker.kt`, `listener/MissionListener.kt`, `hud/MissionHud.kt`, `notify/MissionNotifier.kt`, `command/MissoesCommand.kt` (admin + fallback de chat)
- Modify: `OceanBlockPlugin.kt` (`MissionsSystem.init` logo após `LevelSystem.init`; `MissionsSystem.onDisable()`)
- Test: `src/test/kotlin/ocean_block/system/missions/{MissionRulesTest,DailyRollTest,MissionTextTest,MissionTrailTest}.kt`

**Interfaces:**
- Consumes: eventos da Task 2, `CustomCraftEvent`, `SellEvent` (valor do stack vendido), `PlayerLevelUpEvent`, `PlayerLevels.award/levelOf`, `LevelCurve.toNext`, `HudApi.show/update/hide`, `IslandManager.getIslandByPlayer`, `Island.getUpgradeLevel(IslandUpgrade.Generator)`.
- Produces:
  - `Mission(id: String, text: String, objective: MissionObjective, xp: Int)`.
  - `sealed interface MissionObjective { val goal: Long }`: `CollectDebris(amount: Int, hookOnly: Boolean)`, `BuildGenerator`, `BreakGeneratorBlocks(amount: Int, material: String?)`, `Craft(recipeId: String)`, `CraftAny(amount: Int)`, `ReachLevel(level: Int)`, `Sell(value: Long)`, `IslandUpgrade(upgrade: String, level: Int)`.
  - `sealed interface MissionSignal`: `Debris(byHook: Boolean)`, `GeneratorBlock(material: String)`, `GeneratorFormed`, `Crafted(recipeId: String)`, `Sold(value: Long)`, `State(level: Int, upgrades: Map<String, Int>?, hasGenerator: Boolean)` (`upgrades == null` = sem ilha).
  - `PlayerMissions(uuid: String, trailIndex: Int, trailProgress: Long, dailyDate: String, dailies: List<DailyMission>)`, `DailyMission(id: String, progress: Long, xp: Int, done: Boolean)`.
  - `MissionRules.apply(state: PlayerMissions, signal: MissionSignal): Pair<PlayerMissions, List<Completed>>` com `Completed(mission: Mission, xp: Int)`; `MissionRules.needsReset(state, today: LocalDate): Boolean`; `MissionRules.rollDailies(today: LocalDate, level: Int, random: Random): List<DailyMission>`; `MissionRules.current(state): Current?` (`Current(mission, progress, dailyIndex: Int?)`).
  - `MissionText.hud(current: Current?, trailDone: Boolean): String`, `MissionText.counter(objective, progress): String`.

- [ ] **Step 1: Testes que falham**
  - `MissionTrailTest`: 13 missões, ids únicos, textos e XP exatamente da tabela da spec; todo `Craft(recipeId)` está entre `gancho, rede, fundicao, fundicao_melhorada, fundicao_funil, picareta_fundidora, gerador_energia, painel_solar, fornalha_eletrica, minerador`.
  - `MissionRulesTest`:
    - missão 1: 9 sinais `Debris(byHook = true)` deixam `trailProgress = 9`; `Debris(false)` não conta; o 10º conclui com XP 50 e `trailIndex = 1`, `trailProgress = 0`.
    - missão 2 conclui com `GeneratorFormed` e também com `GeneratorBlock("COBBLESTONE")`.
    - missão 10 só conta `GeneratorBlock("IRON_ORE")`.
    - missão 8 soma `Sold(350)` + `Sold(700)` → conclui.
    - cadeia: índice 3 (missão 4) recebendo `Crafted("fundicao")` conclui a 4; em seguida `State(level = 4, upgrades = emptyMap(), hasGenerator = false)` conclui a 5 (`ReachLevel(3)`); e `State(level = 30, upgrades = mapOf("Generator" to 1), ...)` no índice 4 conclui só a 5 e para na 6 (`Craft`).
    - sem ilha: índice 8 (missão 9) com `State(level = 30, upgrades = null, hasGenerator = false)` não conclui e não lança.
    - ilha já com upgrade: índice 8 com `State(..., upgrades = mapOf("Generator" to 1), ...)` conclui.
    - diárias não contam antes do fim da trilha (`trailIndex < 13`, sinais não mexem em `dailies`).
    - com a trilha feita e 3 diárias, um sinal conta só na primeira não concluída que aceita aquele sinal; concluída marca `done = true` e devolve o XP guardado.
    - `needsReset` é `true` com `dailyDate` "2026-10-24" e hoje 2026-10-25, `false` no mesmo dia, `false` antes do fim da trilha.
  - `DailyRollTest`: `rollDailies(dia, 10, Random(1))` dá 3 ids distintos do `DailyPool`, nunca dois do mesmo grupo (`gerador_64`/`gerador_128` e `venda_2000`/`venda_5000` não saem juntas); XP = `max(30, round(0.15 × toNext(10)))` = 474; com nível 1, XP = 30 (15 arredonda para baixo do mínimo).
  - `MissionTextTest`: trilha `<aqua>Missão:</aqua> <white>Colete 10 detritos usando o gancho</white> <gray>3/10`; venda `<gray>$350/$1.000`; missão sem contador (BuildGenerator, Craft) sem o trecho cinza; diária `<aqua>Diária 2/3:</aqua> ...`; tudo feito `<gray>Diárias concluídas. Volte amanhã!`.
- [ ] **Step 2: Rodar** `./gradlew :plugins:oceanblock:system:missions:localTest` → FAIL.
- [ ] **Step 3: Implementar**
  - Catálogos com os valores da spec. `rollDailies` embaralha o pool com `random`, pega em ordem pulando ids cujo grupo já saiu (grupo = id sem o sufixo numérico: `detritos`, `gancho`, `gerador`, `receitas`, `venda`).
  - `MissionRules.apply`: `State` checa `ReachLevel`, `IslandUpgrade` e `BuildGenerator` (com `hasGenerator`) e encadeia enquanto a atual for de estado e estiver cumprida. O tracker sempre manda um `State` depois de qualquer conclusão, então `apply` não guarda estado entre sinais.
  - `MissionRepository`/`MissionManager` no formato do `LevelManager` (coleção `player_missions`, índice único `uuid`).
  - `MissionTracker.signal(player, signal)`: ignora criativo/espectador; reseta diárias se `needsReset` (data `LocalDate.now(ZoneId.of("America/Sao_Paulo"))`, sorteio com o nível atual); aplica; dá XP e notifica cada `Completed`; se houve conclusão, aplica `stateOf(player)`; atualiza o HUD. `stateOf` lê nível, ilha (`IslandManager.getIslandByPlayer`) e upgrades (`IslandUpgrade.entries.associate { it.name to island.getUpgradeLevel(it) }`), com `hasGenerator = false`: a missão 2 conclui pelos sinais `GeneratorFormed`/`GeneratorBlock`.
  - `MissionListener`: os seis eventos viram sinais; `GeneratorFormedEvent(island)` manda `GeneratorFormed` para `island.getIslandOnlinePlayers()`; `IslandUpgradeEvent` manda `State` para os online da ilha; `PlayerJoinEvent` (1 tick depois) manda `State` e mostra o HUD; `PlayerQuitEvent` esconde o HUD; tarefa a cada 1200 ticks checa a virada de dia dos online.
  - `MissionHud`: `HudApi.show(player, "missao", MissionText.hud(...))`.
  - `MissionNotifier`: região `avisos`, chave `missao`, 100 ticks, `<green>Missão concluída:</green> <white>texto</white> <gold>+N XP`, ícone `item:map`, som `UI_TOAST_CHALLENGE_COMPLETE`.
  - `MissoesCommand` (`@CommandAlias("missoes|missao")`): `@Default` por enquanto escreve a missão atual no chat; `pular` (conclui a atual sem XP) e `resetar` com `oneblock.admin`.
- [ ] **Step 4: Rodar** `./gradlew :plugins:oceanblock:system:missions:localTest` e `./gradlew :plugins:oceanblock:plugin:localJar` → PASS.
- [ ] **Step 5: Commit** `feat(missions): trilha de missões, diárias e HUD`.

### Task 7: Diálogo `/missoes` e botão no menu da ilha

**Files:**
- Create: `server/plugins/Textura/configs/ui/missoes.json` (tema `mare`)
- Create: `plugins/oceanblock/system/missions/src/main/kotlin/ocean_block/system/missions/dialog/{MissionDialog,MissionLore}.kt`
- Modify: `MissoesCommand.kt` (`@Default` abre o diálogo; chat como fallback), `IslandView.kt` (botão `MAP` "Missões" / "Mostra sua missão e as diárias." rodando `missoes`, num slot livre do hub)
- Test: `src/test/kotlin/ocean_block/system/missions/MissionLoreTest.kt`

**Interfaces:**
- Consumes: `MissionRules.current`, `MissionTrail`, `PlayerMissions`.
- Produces: `MissionLore.trail(state): Map<String, String>` (chaves `titulo`, `texto`, `contador`, `xp`, `barra`), `MissionLore.daily(daily: DailyMission): Map<String, String>` (chaves `texto`, `contador`, `xp`, `barra`, `feito`), `MissionLore.resetIn(now: LocalDateTime): String`.

- [ ] **Step 1: Teste que falha** — `MissionLoreTest`: trilha no índice 0 com progresso 3 → `titulo` "Missão 1 de 13", `contador` "3/10", `xp` "+50 XP", `barra` "30"; trilha completa → `titulo` "Trilha concluída!"; diária feita → `feito` "true", `barra` "100"; `resetIn(23:15)` → "Renovam em 00:45".
- [ ] **Step 2: Rodar** `./gradlew :plugins:oceanblock:system:missions:localTest --tests "*MissionLore*"` → FAIL.
- [ ] **Step 3: Implementar** a tela (card "Trilha" com `progress`, card "Diárias" com lista `diarias` de 3 e a linha `{renova}`; antes do fim da trilha o card mostra `Termine a trilha para liberar as diárias.` via placeholder condicional) e `MissionDialog.open` como o `LevelDialog`.
- [ ] **Step 4: Rodar** testes, `localJar`, prévia no Studio → PASS.
- [ ] **Step 5: Commit** `feat(missions): diálogo /missoes e atalho no menu da ilha`.

### Task 8: Documentação

**Files:**
- Modify: `CLAUDE.md` (seção Research → Level e Missions; `research =` em Recipes/Stations/Machines → `level`; lista de módulos com testes; menu da ilha; tema Maré no Textura)
- Modify: `docs/superpowers/plans/2026-10-18-oceanblock-aceite-manual.md` (seção de pesquisa vira nível e missões: XP por fonte, Rede/Minerador sem XP, colocar-e-quebrar sem XP, subida com título/som/aviso, `/receitas` travado, sidebar, `/nivel`, HUD da missão, cadeia 1→13, diárias, virada de dia, `/missoes`, admin)

- [ ] **Step 1:** Editar os dois arquivos.
- [ ] **Step 2:** `grep -rn "pesquisa\|research" CLAUDE.md docs/superpowers/plans/2026-10-18-oceanblock-aceite-manual.md` só mostra menções históricas intencionais.
- [ ] **Step 3: Commit** `docs(progressao): nível e missões no CLAUDE.md e no manual de aceite`.

### Task 9: Deploy e verificação

- [ ] **Step 1:** Rodar todos os testes tocados: `./gradlew :plugins:oceanblock:system:craft:test :plugins:oceanblock:system:drift:test :plugins:oceanblock:system:islands:test :plugins:oceanblock:system:stations:test :plugins:oceanblock:system:machines:test :plugins:oceanblock:system:level:localTest :plugins:oceanblock:system:missions:localTest :plugins:textura:localTest` → BUILD SUCCESSFUL.
- [ ] **Step 2:** Pedir ao usuário o sim para parar o servidor.
- [ ] **Step 3:** `docker compose stop servidor`, `./gradlew :plugins:oceanblock:plugin:localDeploy :plugins:textura:localDeploy`, `docker compose start servidor`.
- [ ] **Step 4:** `docker logs oceanblock-servidor` até `Done (` e `Hash:`; conferir sem `[nivel]`/`[missao]` severos nem exceções novas; `/textura gerar` se o log pedir.
- [ ] **Step 5:** Commit de qualquer ajuste final.
