# OceanBlock — Deriva + Gancho — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Detritos derivam por cada lote ativo e o jogador os coleta com o item Gancho ou por toque, recebendo os drops do tipo.

**Architecture:** Módulo novo `plugins/oceanblock/system/drift` (pacote `ocean_block.system.drift`). Unidades puras (config, sorteio, trilha, regra de coleta) testadas com Kotest; um `DebrisManager` com um task global move `ItemDisplay`s não persistentes; o Gancho é um `CustomItem` registrado no framework `custom-items` com um listener de `PlayerFishEvent`.

**Tech Stack:** Kotlin 2.3, Paper 26.3 (`ItemDisplay`, `PlayerFishEvent`, `ShapedRecipe`), Kotest 5.9, Bukkit `YamlConfiguration`.

**Spec:** `docs/superpowers/specs/2026-10-18-oceanblock-deriva-gancho-design.md`

## Global Constraints

- Sem comentários em código, Gradle ou YAML gerado. Strings ao jogador em português.
- Commits `type(scope): description` terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Nunca `git commit -n` nem `--no-verify` (hook bloqueia; evite `-n` em qualquer lugar do mesmo comando do commit).
- Build: `./gradlew :plugins:oceanblock:plugin:localJar`. Testes do módulo: `./gradlew :plugins:oceanblock:system:drift:test -Penv=local`.
- Padrões: corrente `EAST`, velocidade `0.05` bloco/tick, trilha `40`, largura lateral `10`, `y = 62.6`, spawn a cada `100` ticks, máximo `8` por lote, preso some em `60` s, raio do gancho `2.0`, raio de toque `1.5`, puxão `10` ticks, task de movimento a cada `2` ticks, action bar `40` ticks, `Channel.Drift(15)`.
- Tipos, pesos e drops exatamente como a tabela 2.4 da spec. Resultado de sorteio vazio vira `STRING` x1.
- Gancho: id `gancho`, `FISHING_ROD`, nome `<white>Gancho`, lore `<gray>Lance na água e recolha` / `<gray>para puxar detritos.`; receita `"  P" / " PS" / "P S"`, `P` = `Tag.PLANKS`, `S` = `STRING`, chave `NamespacedKey("oceanblock", "gancho")`.
- Coleta só por dono, membro ou trust do lote (comparação de nome em minúsculas).
- Detritos: `ItemDisplay`, `persistent = false`, `teleportDuration = 2`; todos removidos em `DriftSystem.onDisable()`.

## Review Focus

- **Lote fica vazio / jogador desloga**: detritos do lote somem no tick seguinte (Task 5).
- **Reset ou remoção do lote com detritos ativos**: detritos órfãos removidos (Task 5, checagem de `IslandManager` por id).
- **YAML com material inválido ou sem tipos**: log e deriva desligada sem derrubar o plugin (Task 1, teste do parser).
- **Inventário cheio na entrega**: sobra cai aos pés do jogador (Task 4).
- **Dois jogadores puxando o mesmo detrito**: só o primeiro recebe; detrito em puxão não é elegível de novo (Task 5/6, `pullTarget` checado).

---

### Task 1: Módulo `drift`, config e wiring

**Files:**
- Create: `plugins/oceanblock/system/drift/build.gradle.kts`
- Create: `plugins/oceanblock/system/drift/src/main/kotlin/ocean_block/system/drift/config/{CurrentDirection,DropEntry,DebrisType,DriftSettings,DriftConfig,DriftConfigParser,DriftFile}.kt`
- Create: `plugins/oceanblock/system/drift/src/main/kotlin/ocean_block/system/drift/DriftSystem.kt`
- Create: `plugins/oceanblock/system/drift/src/main/resources/drift.yml`
- Test: `plugins/oceanblock/system/drift/src/test/kotlin/ocean_block/system/drift/config/DriftConfigParserTest.kt`
- Modify: `settings.gradle.kts` (incluir `:plugins:oceanblock:system:drift`)
- Modify: `lib/actionbar/src/main/kotlin/ocean_block/libs/actionbar/Channel.kt` (`Drift(15)`)
- Modify: `plugins/oceanblock/plugin/src/main/kotlin/io/github/badnotice/ocean_block/OceanBlockPlugin.kt`

**Interfaces:**
- Produces:
```kotlin
enum class CurrentDirection(val dx: Int, val dz: Int) { EAST(1, 0), WEST(-1, 0), SOUTH(0, 1), NORTH(0, -1) }
data class DropEntry(val material: Material, val min: Int, val max: Int, val chance: Double)
data class DebrisType(val id: String, val weight: Int, val display: Material, val scale: Float, val drops: List<DropEntry>)
data class DriftSettings(
    val direction: CurrentDirection, val speed: Double, val laneLength: Double, val laneWidth: Double,
    val surfaceY: Double, val spawnIntervalTicks: Long, val maxPerLot: Int, val stuckSeconds: Int,
    val hookRadius: Double, val touchRadius: Double,
)
data class DriftConfig(val settings: DriftSettings, val types: List<DebrisType>)
object DriftConfigParser { fun parse(root: ConfigurationSection, warn: (String) -> Unit): DriftConfig? }
object DriftFile { fun load(plugin: JavaPlugin): DriftConfig? }
object DriftSystem { fun init(plugin: JavaPlugin); fun onDisable() }
```
- YAML (chaves em português): `corrente.{direcao,velocidade,comprimento,largura,altura}`, `spawn.{intervalo-ticks,maximo-por-lote,tempo-preso-segundos}`, `coleta.{raio-gancho,raio-toque}`, `detritos.<id>.{peso,exibicao,escala,drops: [{material,min,max,chance}]}`.

- [ ] **Step 1: Escrever os testes do parser**

`StringSpec` com `YamlConfiguration.loadConfiguration(StringReader(yaml))`:
- `"yaml padrao gera quatro tipos com pesos da spec"`: lê o `drift.yml` de resources (via classloader) → `types.map { it.id to it.weight }` == `listOf("tabua" to 50, "folhas" to 25, "sucata" to 17, "barril" to 8)`; `settings.direction == EAST`, `speed == 0.05`, `maxPerLot == 8`, `surfaceY == 62.6`.
- `"material invalido e ignorado com aviso"`: tipo com `exibicao: NAO_EXISTE` → tipo ausente do resultado e `warn` chamado uma vez com o id.
- `"drop com material invalido some mas o tipo fica"`: drops `[NAO_EXISTE, STICK]` → tipo com 1 drop.
- `"sem tipos validos retorna null"`.
- `"direcao invalida usa EAST com aviso"`.

- [ ] **Step 2: Rodar e ver falhar** — `./gradlew :plugins:oceanblock:system:drift:test -Penv=local` → FAIL (`Unresolved reference`).

- [ ] **Step 3: Implementar config, `drift.yml` e módulo**

`build.gradle.kts` no padrão de `islands` (`paper-common`; `compileOnly` em `projects.plugins.oceanblock.system.islands`, `projects.plugins.oceanblock.system.customItems`, `projects.lib.{instance,actionbar,formatter,itemBuilder,coordinate}`, `files(rootProject.file("jars/FastAsyncWorldEdit-Bukkit-2.15.1.jar"))` só se o compilador exigir por tipos transitivos de `islands`; `testImplementation(libs.paperApi)`, `libs.kotest.runner`, `libs.kotest.assertions`; `tasks.test { useJUnitPlatform() }`). `drift.yml` com os valores da spec 2.4 e do Global Constraints. `DriftFile.load`: `plugin.saveResource("drift.yml", false)` se não existir, depois `DriftConfigParser.parse(YamlConfiguration.loadConfiguration(file), Instance.logger::warning)`; `null` → log `severe` `"[deriva] drift.yml sem tipos validos; deriva desligada."`. `DriftSystem.init` só carrega a config nesta task e guarda em `lateinit var config` / flag `enabled`; `onDisable` vazio por ora. `Channel.Drift(15)`. No plugin: `{ DriftSystem.init(this) }` na lista de `setupSystems` logo depois de `IslandsSystem`, e `DriftSystem.onDisable()` em `onDisable`.

- [ ] **Step 4: Rodar testes e build** — os 5 testes PASS; `localJar` `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit** — `feat(drift): modulo de deriva com config e canal de action bar`.

### Task 2: Sorteio de tipo e de drops (TDD)

**Files:**
- Create: `.../drift/loot/DebrisPicker.kt`, `.../drift/loot/DropRoller.kt`, `.../drift/loot/DebrisPool.kt`
- Test: `.../drift/loot/DebrisPickerTest.kt`, `.../drift/loot/DropRollerTest.kt`

(`...` = `plugins/oceanblock/system/drift/src/{main,test}/kotlin/ocean_block/system/drift`)

**Interfaces:**
- Consumes: `DebrisType`, `DropEntry` (Task 1).
- Produces:
```kotlin
object DebrisPicker { fun pick(types: List<DebrisType>, random: Random): DebrisType? }
object DropRoller { fun roll(type: DebrisType, random: Random): List<Pair<Material, Int>> }
fun interface DebrisPool { fun typesFor(island: Island): List<DebrisType> }
class ConfigDebrisPool(private val types: List<DebrisType>) : DebrisPool
```
(`Random` = `kotlin.random.Random`.)

- [ ] **Step 1: Testes**
- `"lista vazia retorna null"`; `"peso zero nunca e sorteado"` (tipos A peso 0, B peso 1, 1000 sorteios com `Random(1)` → só B); `"frequencia segue o peso"` (A 3, B 1, 4000 sorteios `Random(7)` → A entre 2850 e 3150).
- `"chance 1 sempre cai com quantidade no intervalo"` (drop 1–3 chance 1.0, 500 rolagens → sempre presente, quantidade em 1..3, e 1 e 3 aparecem); `"chance 0 nunca cai"`; `"resultado vazio vira um barbante"` (tipo com drop chance 0 → `listOf(Material.STRING to 1)`).

- [ ] **Step 2: Ver falhar.** **Step 3: Implementar** (`pick` por soma acumulada de pesos > 0). **Step 4: Ver passar.**
- [ ] **Step 5: Commit** — `feat(drift): sorteio de detritos e drops`.

### Task 3: Geometria da trilha (TDD)

**Files:**
- Create: `.../drift/lane/LanePoint.kt`, `.../drift/lane/DriftLane.kt`
- Test: `.../drift/lane/DriftLaneTest.kt`

**Interfaces:**
- Consumes: `CurrentDirection` (Task 1).
- Produces:
```kotlin
data class LanePoint(val x: Double, val z: Double)
object DriftLane {
    fun spawnPoint(originX: Double, originZ: Double, direction: CurrentDirection, laneLength: Double, lateral: Double): LanePoint
    fun advance(point: LanePoint, direction: CurrentDirection, distance: Double): LanePoint
    fun isPastEnd(point: LanePoint, originX: Double, originZ: Double, direction: CurrentDirection, laneLength: Double): Boolean
}
```
`spawnPoint` = origem − direção·(laneLength/2) + perpendicular·lateral, com perpendicular `(-dz, dx)`.

- [ ] **Step 1: Testes**
- `"EAST nasce a oeste do centro com lateral em z"`: origem (1000, 0), EAST, 80, lateral 5 → `LanePoint(960.0, 5.0)`.
- `"NORTH nasce ao sul"`: origem (0, 0), NORTH, 80, lateral 3 → `LanePoint(3.0, 40.0)`.
- `"advance anda na direcao"`: (960, 5) EAST 0.1 → (960.1, 5.0).
- `"fim da trilha"`: EAST, origem (1000, 0), 80 → `isPastEnd((1040.1, 0))` true, `isPastEnd((1039.9, 0))` false, `isPastEnd((960, 0))` false.
- [ ] **Step 2–4: falhar, implementar, passar.**
- [ ] **Step 5: Commit** — `feat(drift): geometria da trilha de deriva`.

### Task 4: Regra de coleta e entrega

**Files:**
- Create: `.../drift/collect/CollectorRule.kt`, `.../drift/collect/DebrisDelivery.kt`
- Test: `.../drift/collect/CollectorRuleTest.kt`

**Interfaces:**
- Produces:
```kotlin
object CollectorRule {
    fun canCollect(ownerUuid: UUID, members: Set<String>, trusted: Set<String>, playerUuid: UUID, playerName: String): Boolean
    fun canCollect(island: Island, player: Player): Boolean
}
object DebrisDelivery { fun deliver(player: Player, drops: List<Pair<Material, Int>>) }
```

- [ ] **Step 1: Testes** (`canCollect` puro): dono → true; membro com nome em caixa diferente → true; trust → true; estranho → false.
- [ ] **Step 2–4: falhar, implementar, passar.** A sobrecarga com `Island` usa `island.ownerUuid`, `island.members.keys`, `island.trustedNames`. `DebrisDelivery.deliver`: `player.inventory.addItem(...)` por par; sobras `player.world.dropItem(player.location, it)`; `Sound.ENTITY_ITEM_PICKUP`; `Particle.SPLASH` (20) na localização do jogador; `ActionBar.send(player, Channel.Drift, Mini.deserialize(texto), 40)` com texto `"<green>+N <lang:CHAVE>"` por par separados por `"<gray>  "`, `CHAVE` = `material.translationKey()`.
- [ ] **Step 5: Commit** — `feat(drift): regra de coleta e entrega de drops`.

### Task 5: Runtime dos detritos

**Files:**
- Create: `.../drift/debris/Debris.kt`, `.../drift/debris/DebrisManager.kt`, `.../drift/debris/DebrisEntities.kt`
- Modify: `.../drift/DriftSystem.kt`

**Interfaces:**
- Consumes: Tasks 1–4.
- Produces:
```kotlin
class Debris(val islandId: UUID, val type: DebrisType, val entity: ItemDisplay) {
    var point: LanePoint; var stuckSinceMillis: Long?; var pullTarget: UUID?; var pullTicksLeft: Int
}
object DebrisManager {
    fun start(config: DriftConfig, pool: DebrisPool)
    fun stop()
    fun nearestFree(location: Location, radius: Double): Debris?
    fun beginPull(debris: Debris, player: Player)
}
object DebrisEntities { fun spawn(world: World, point: LanePoint, y: Double, type: DebrisType): ItemDisplay }
```

- [ ] **Step 1: Implementar**
- `DebrisEntities.spawn`: `world.spawn(Location(world, x, y, z), ItemDisplay::class.java) { it.setItemStack(ItemStack(type.display)); it.isPersistent = false; it.teleportDuration = 2; it.transformation` com escala `type.scale` }.
- `start`: dois `runTaskTimer`: spawn a cada `spawnIntervalTicks` e movimento a cada `2` ticks.
- Lotes ativos (calculado em cada tick de movimento): `Bukkit.getOnlinePlayers().mapNotNull { IslandManager.getIslandAt(it.location) }.toSet()`.
- Spawn: para cada lote ativo com menos de `maxPerLot` detritos, tipo = `DebrisPicker.pick(pool.typesFor(island), Random)`, lateral uniforme em `[-laneWidth, laneWidth]`, ponto `DriftLane.spawnPoint(island.position.originX, island.position.originZ, ...)`.
- Movimento, por detrito: remove se o lote não está ativo ou `IslandManager` não tem mais o id (`getIslandByOwnerUUID` não serve; use o mapa de ilhas por id do manager, expondo um `fun getIsland(uuid: UUID): Island?` em `IslandManager` se não existir), se a entidade morreu, se passou do fim, ou se `stuckSinceMillis` passou de `stuckSeconds`. Em puxão: move `1/pullTicksLeft` do caminho até `player.location` por tick de movimento (conta 2 ticks por iteração), ao zerar entrega `DropRoller.roll` via `DebrisDelivery` ao jogador alvo (se online) e remove. Livre: próximo ponto `advance(point, direction, speed * 2)`; se o bloco em `(floor(x), 62, floor(z))` não for `WATER` nem ar → marca preso e não anda; senão teleporta para `(x, surfaceY, z)`.
- Toque: para cada detrito livre, jogador online a `<= touchRadius` com `CollectorRule.canCollect(island, player)` → entrega e remove.
- `nearestFree`: detrito sem `pullTarget` mais próximo dentro do raio, mesmo mundo.
- `stop`: cancela tasks, remove todas as entidades, limpa listas.
- `DriftSystem.init`: se config válida, `DebrisManager.start(config, ConfigDebrisPool(config.types))`; `onDisable` chama `DebrisManager.stop()`.
- [ ] **Step 2: Build** — `localJar` `BUILD SUCCESSFUL`; testes do módulo e de `islands` PASS.
- [ ] **Step 3: Commit** — `feat(drift): detritos derivando pelos lotes ativos`.

### Task 6: Gancho

**Files:**
- Create: `.../drift/hook/GanchoItem.kt`, `.../drift/hook/GanchoListener.kt`, `.../drift/hook/GanchoRecipe.kt`, `.../drift/hook/FirstJoinListener.kt`
- Modify: `.../drift/DriftSystem.kt`

**Interfaces:**
- Consumes: `CustomItem`, `CustomItemRegistry` (custom-items), `DebrisManager.nearestFree/beginPull`, `CollectorRule`.
- Produces: `object GanchoItem : CustomItem()` (valores do Global Constraints), `listeners()` = `listOf(GanchoListener())`.

- [ ] **Step 1: Implementar**
- `GanchoListener.onFish(PlayerFishEvent)`: só se `CustomItemRegistry.fromItemStack(player.inventory.itemInMainHand) == GanchoItem` (ou mão secundária); estados `REEL_IN`, `FAILED_ATTEMPT`, `IN_GROUND`; ilha em `IslandManager.getIslandAt(event.hook.location)`, exige `CollectorRule.canCollect`; `DebrisManager.nearestFree(event.hook.location, hookRadius)?.let { DebrisManager.beginPull(it, player) }`. Não cancela o evento.
- `GanchoRecipe.register()`: `ShapedRecipe(NamespacedKey("oceanblock", "gancho"), GanchoItem.build())`, forma e ingredientes do Global Constraints (`RecipeChoice.MaterialChoice(Tag.PLANKS)`), `Bukkit.addRecipe` só se `Bukkit.getRecipe(key) == null`.
- `FirstJoinListener`: `PlayerJoinEvent` com `!player.hasPlayedBefore()` → `player.inventory.addItem(GanchoItem.build())`.
- `DriftSystem.init`: `CustomItemRegistry.register(GanchoItem)`, `GanchoRecipe.register()`, registra `FirstJoinListener`, mesmo com a deriva desligada.
- [ ] **Step 2: Build e testes** — `localJar` e testes do módulo PASS; `/citem give gancho` é coberto no aceite.
- [ ] **Step 3: Commit** — `feat(drift): item gancho, receita e entrega no primeiro login`.

### Task 7: Documentação e aceite

**Files:**
- Modify: `CLAUDE.md` (parágrafo do módulo `drift` em "System module pattern" ou seção própria: o que faz, unidades, `Channel.Drift`, `drift.yml`, `DebrisPool` como seam do subprojeto 3)
- Modify: `docs/superpowers/plans/2026-10-18-oceanblock-aceite-manual.md` (seção "7. Deriva e gancho" com: detritos passando perto do raft; puxar tábua com gancho, ver respingo, som e action bar; coletar nadando; detrito preso na borda some em 60 s; barril dá loot; receita do gancho na bancada; visitante sem trust não coleta; sair do lote limpa detritos; restart sem detritos sobrando; `drift.yml` criado em `server/plugins/ocean-block/`; novo jogador recebe o gancho)

- [ ] **Step 1: Escrever** · **Step 2: Commit** — `docs(drift): documenta deriva e roteiro de aceite`.
