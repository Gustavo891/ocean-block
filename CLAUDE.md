# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a Gradle multi-module monorepo (Kotlin/JVM 2.3.0, JDK 25 toolchain, Paper 26.3). The shippable artifact is the `one-block` Paper plugin, assembled by shading many internal modules into one jar.

**Primary build+deploy command** (builds the shaded jar and copies it into `server/plugins/`):

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

- `localJar` / `localDeploy` come from the `paper-plugin` convention. `productionJar` builds the prod jar into `build/libs` only.
- The dev server (Paper) must be **stopped** before deploying — Windows locks the running jar and the copy fails. `localDeploy` intentionally does not track state (its destination `server/plugins/` holds other plugins' runtime data, e.g. LuckPerms' H2 DB).
- After deploying, the server must be **restarted** (or the plugin reloaded) to pick up the new jar.

### The `isLocal` mechanism — read before running any Gradle task

The build mode is derived from whether **any requested task name contains the substring `local`** (case-insensitive), via `gradle.startParameter.taskNames`. This flag (`isLocal`) changes dependencies and source sets in `plugins/oneblock/plugin` and `lib/connection`:

| | local mode (task name has "local") | production mode (anything else) |
|---|---|---|
| `com.redescreen:sdk:1.0` | skipped | required (**only exists in the prod environment**) |
| MongoDB driver | `implementation` (shaded) | `compileOnly` |
| `lib:connection` source | `src/local/kotlin` | `src/production/kotlin` |

`lib:connection` additionally accepts `-Penv=local` as an override, so `./gradlew :lib:connection:build -Penv=local` works; the plugin module honors task names only.

Consequence: a plain `./gradlew build`, `test`, or `:some:module:compileKotlin` runs in **production mode** and fails resolving `com.redescreen:sdk:1.0`. Always drive local work through a `local*` task. To compile everything locally, `:plugins:oneblock:plugin:localDeploy` transitively builds all modules.

### Local dependency jars (not on any repo)

Three paths are wired as file dependencies and must exist on disk or the build fails with unresolved symbols rather than a dependency error:

- `jars/inventory-framework-paper-3.7.1.jar` — inventory-framework, `compileOnly` for every module via `paper-common`, `implementation` (shaded) in both plugin modules. The version catalog entry `inventoryFramework` is unused.
- `jars/ModelEngine-4.1.0.jar` — ModelEngine (`com.ticxo.modelengine.api`), `compileOnly` by `system/minions` only. A runtime `softDepend`, so `MinionModel.isAvailable()` gates every call and minions fall back to armor-stand cosmetics without it.
- `.dep/` — a `fileTree` of server-only APIs (currently `SkinsRestorer.jar`), consumed `compileOnly` by `system/islands`.

`jars/packetevents-spigot-2.13.0.jar` is **not** one of them — packetevents resolves from Maven (`repo.codemc.io`) via the `packetevents-spigot` catalog entry. That jar is just the copy you drop into `server/plugins/`; it is a runtime `softDepend`, so the build succeeds without the server plugin but `lib:toast` silently no-ops (logs once) if PacketEvents is missing at runtime.

### Tests

Tests use **Kotest 5.9 on the JUnit 5 platform**. `paper-common` does *not* wire testing — a module that wants tests must add `testImplementation(libs.kotest.runner)` + `libs.kotest.assertions` (plus `libs.paperApi` when the code under test touches Bukkit types) and `tasks.test { useJUnitPlatform() }` to its own `build.gradle.kts`. Only `plugins/textura`, `system/custom-items`, and `system/islands` have tests today. Tests live under `src/test/kotlin`.

```bash
./gradlew :plugins:oneblock:system:custom-items:test
./gradlew :plugins:textura:localTest
./gradlew :plugins:textura:test --tests "*CooldownStore*"
```

`plugins/textura` also registers a `localTest` alias whose only purpose is to put `local` in the task name so the build resolves in local mode. Modules that do **not** depend on `lib:connection` test cleanly. A whole-project `test` fails in production mode on the `com.redescreen:sdk` resolution unless that SDK is present.

## Architecture

### Convention plugins (`build-logic/`)

- **`paper-common`** — base for every module: Kotlin + Shadow, paper-api (`26.3.build.192-stable`, Adventure 5 — `ClickEvent<T>` is generic and `net.kyori.examination` is gone, so Textura shades and relocates it for `creative`) and the local inventory-framework jar as `compileOnly`, Aikar ACF (`co.aikar:acf-paper`) as `implementation`, JDK 25, Shadow 9.2.2 (9.3+ needs a newer Gradle; 8.x cannot read Java 25 classes), and the full list of third-party Maven repos. Every module (including `lib:*` and `system:*`) produces a shadowJar with an empty archive classifier.
- **`paper-plugin`** — applies `paper-common` + `plugin-yml.bukkit`, disables the thin `jar` task (it would collide with shadowJar on the same output name), and adds `localJar` / `productionJar` / `localDeploy`. Only the two real plugins apply it. Because the thin `jar` is disabled, the convention also republishes `shadowJar` into `apiElements`/`runtimeElements`: without that, a project that applies `paper-plugin` has **no consumable artifact**, and `compileOnly(projects.plugins.textura)` in `lib:formatter` resolves to nothing — it only compiles while a stale jar happens to sit in `build/libs`, and fails on any clean build.

### Two shippable plugins vs. shaded modules

Only `:plugins:oneblock:plugin` and `:plugins:textura` are standalone plugin jars. **All `:lib:*` and `:plugins:oneblock:system:*` modules are shaded into the oneblock jar** — the plugin module `implementation`s every `:plugins:oneblock:system:*` and `:lib:*` and merges them at `shadowJar` time. `textura` is a separate jar declared in oneblock's `depend` list (`PlaceholderAPI`, `SkinsRestorer`, `Textura`), not shaded — systems that need it (e.g. `islands`) take `compileOnly(projects.plugins.textura)` and resolve it at runtime from the server.

### System module pattern

Each feature lives in its own module under `plugins/oneblock/system/<name>/` and exposes a singleton `object <Name>System` with an `init(...)` function. `OneBlockPlugin.onEnable()` wires everything in three phases:

- `setupSystems()` — calls every `XxxSystem.init(plugin, viewFrame | paperCommandManager)`.
- `setupCommands()` — registers Aikar ACF commands via `PaperCommandManager` (`@CommandAlias`, `@Subcommand`, `@Default`, `@CommandPermission`).
- `setupListeners()` — registers Bukkit `Listener`s.

To add a feature module: register it in `settings.gradle.kts`, add it to the plugin's dependency loop (systems under `:plugins:oneblock:system:` are auto-included), and call its `init` in `setupSystems()`. Those two steps are independent — `chats` is included and shaded but deliberately never `init`ed (its listener would reformat server chat), so it ships inert. Check `setupSystems()` before assuming a module under `system/` is live.

Systems may `compileOnly` on other systems to register into them (e.g. `minions` depends on `craft` and calls `Crafts.register(ForgeRecipe(...))` from its own `init`, and on `islands` for island lookups). Keep that registration inside the depending system's `init`, not in the plugin module.

#### Custom-item framework (`system/custom-items`)

Reusable in-game item behavior lives here — do **not** hand-roll per-item click listeners in other modules. Define a behavior by subclassing `CustomItem` (`api/CustomItem.kt`): override the appearance fields (`id`, `material`, `displayName`, `lore`, `itemModel`, `cooldownMillis`, `heldTickInterval`) and the hooks you need (`onRightClick`, `onLeftClick`, `onInteractEntity`, `onConsume`, `onHeldTick`; plus optional `listeners()` / `tasks()`). `build()` stamps the `id` into item NBT under `CustomItemKeys.ID`; `CustomItemRegistry.fromItemStack(...)` resolves an `ItemStack` back to its `CustomItem` by that tag. Register built-ins in `CustomItemSystem.registerBuiltInItems()` (e.g. `MagnetItem`). A single global tick task in `CustomItemSystem` drives every item's `onHeldTick` at its own `heldTickInterval`; a shared `CooldownStore` gates activations.

#### Progression systems (`system/skills`, `system/battle-pass`, `system/collections`)

Three separate progression tracks, all Manager+Repository backed:

- **`skills`** — five skills (`Skill` enum), leveled by XP from gameplay listeners. Perks are one `Listener` per skill (`MiningPerks`, `LumberPerks`, `FarmingPerks`, `CombatPerks`, `FishingPerks`), all registered in `SkillsSystem.init`. Levels gate crafting through `SkillRecipeGates.registerAll()` (registers into `system/craft`) and are exposed to PlaceholderAPI via `SkillExpansion` — registered only when PlaceholderAPI is present. XP feedback is **batched**: `XpFeedback` accumulates gains and flushes every 5 ticks into the `SkillXp` action-bar channel plus a floating text display, so fast mining doesn't spam. Admin commands operate on **levels** (`/skills setlevel|addxp|reset <player> <skill> ...`) with `skills` tab completion registered in `SkillsSystem.init`.
- **`battle-pass`** — `Challenges` is a **hardcoded enum** (display name, icon `ItemStack`, `xpReward`, and a sealed `ChallengeObjective` such as `BreakOneBlock`, `CustomCraft`, `SellValue`), not YAML. Adding a challenge means adding an enum entry plus the matching branch in `ChallengeListener`. `BattlePassSystem` exposes its `ChallengeListener` as a public `lateinit` so other systems can report progress into it.
- Both, plus `collections`, render their menus with Textura fonts/glyphs — see the Textura section for `TitleApi`.

`system/special-items` (package `one_block.system.special_items`, sell beacon) predates this framework and hand-rolls its own listeners/tasks — legacy; new item behavior goes in `custom-items`, don't copy that shape. `MultiTool` used to live in the plugin module and is now `content/multitool/MultiToolItem` here, registered like any other built-in; `/multitool` still hands it out, and `/citem give multitool` works too.

The **plugin module holds no feature code** — only `OneBlockPlugin`, `OneBlockCommand` and `MobControlListener`. Anything else that lands there is misplaced.

### Dependency direction (important)

Systems depend `compileOnly` on `lib:*` and occasionally on other systems; the **plugin module** depends on all of them to shade. **Systems must never depend on the plugin module** — that inverts the graph. Cross-cutting code that a low-level system would need must live in a `lib:*` module or be passed in via `init`, not reached back up into `plugins/oneblock/plugin`.

### Shared libraries (`lib/`)

- **`instance`** — two global handles set in `onEnable`: `Instance` (`JavaPlugin`, backed by `PluginInstance`) for the Bukkit scheduler / `NamespacedKey` / logging, and `ScheduledExecutor` (single-thread `ScheduledExecutorService`, backed by `ScheduledExecutorInstance`) for off-main-thread periodic work such as repository flushes. Never touch the Bukkit API from `ScheduledExecutor`.
- **`connection`** — data layer (MongoDB), with the env-split source sets described above. Exposes a single lazy `Mongo: MongoDatabase` (database `one-block`) configured with an automatic POJO codec provider, so domain classes persist without hand-written codecs.
- **`coordinate`** — `SimpleLocation` and world-coordinate helpers.
- **`actionbar`** — the single owner of the action bar. **Never call `player.sendActionBar` directly** from a system: whoever writes last wins and short messages get erased by periodic ones. Publish with `ActionBar.send(player, Channel.X, component, durationTicks)`; a 2-tick render task shows the highest-`priority` live channel and re-sends every 20 ticks so it doesn't fade. Add a `Channel` enum entry per system that writes there (`FlyTimer` 0, `Combat` 10, `SkillXp` 20). Safe to call off the main thread.
- **`rewards`** — shared reward model (`Reward(type, value, material, amount)`, `RewardType.Message | Item | Command`) and `RewardGranter.grant(rewards, player(s))`, which handles full inventories (drops leftovers) and `%player%` / `{player}` substitution in commands. Island phases and skill levels both pay through it — new reward-granting features reuse it instead of re-implementing delivery.
- **`toast`** — advancement-toast popups, sent as raw `WrapperPlayServerUpdateAdvancements` packets through packetevents (grant then revoke 5 ticks later). Only `ToastApi.send(...)` is public; `ToastService` is `internal`. Add a `ToastType` entry per use case — the enum maps to the advancement frame.
- **`formatter`**, **`translation`**, **`item-builder`** (`Items` / `ItemStackExt`) — text, i18n, item construction.

### GUIs and text

- GUIs use **inventory-framework** (`me.devnatan.inventoryframework`): views are registered with `viewFrame.with(...)` and the frame is created once in `OneBlockPlugin`.
- Text uses **Adventure MiniMessage**. Use `one_block.libs.formatter.Mini.mm` (or `Mini.deserialize(...)`), **not** `MiniMessage.miniMessage()` — `Mini.mm` is the shared instance carrying Textura's `<glyph:id>` / `<shift:n>` tags, which the default instance would emit as literal text. Remember `.decoration(TextDecoration.ITALIC, false)` on item display names.
- Item behavior uses **Paper data components** (`io.papermc.paper.datacomponent.*`, e.g. `CAN_BREAK`, `TOOL`, `RegistrySet`/`RegistryKey`). Verify exact builder signatures against the paper-api jar rather than guessing — the API churns between versions.

### Runtime configuration & data

- Config and runtime data live under `server/plugins/` (not shaded into jars). The oneblock progression is data-driven: `server/plugins/one-block/phases/*.yml` define ordered phases, each with a weighted `block-pool`, `mob-pool`, `monster-party`, and a `completion:` block (`announce:` lines + `rewards:` of `type: Message | Item | Command`). `PhaseManager.loadPhases()` parses them; invalid materials/entities are logged and skipped, not fatal.

#### Phase advancement (`system/islands/phase` + `reward`)

`PhaseAdvancement.advance(island)` is the single entry point when an island fills its grid. It freezes the island (`frozen` set, so a double-trigger is a no-op), grants rewards, announces in chat to online members, runs `PhaseAnimation` on the center block, then after `PhaseAnimation.FREEZE_TICKS` reveals the next phase and marks the island dirty. Rewards go to the **island owner only** — `PhaseRewarder.grantOrQueue` queues into `island.pendingRewardPhases` when the owner is offline, and `drainPending` flushes on their next join. Don't call `PhaseRewarder` or mutate `currentPhaseId` from elsewhere.
- Persistent state is stored in MongoDB via `lib:connection`.

### Persistence pattern (Manager + Repository)

Stateful systems follow the same shape — read `islands` or `minions` for a full example:

- `class XRepository` — owns one `Mongo.getCollection<X>("...")`, creates its unique index in `init`, and exposes `toList()` / `update(x)` / `updateAll(list)` (bulk `ReplaceOneModel` with `upsert`) / `remove(uuid)`. No business logic.
- `object XManager` — the single source of truth at runtime. `onInit()` loads the whole collection into in-memory maps; mutations touch the maps and mark ids in a `dirtyIds` set; a `ScheduledExecutor.scheduleAtFixedRate` job flushes only the dirty entries (islands: every 30s) via `updateAll`. Lookups go through the manager's maps, never through a fresh query.
- Managers with unsaved state expose `onDisable()` and **must be called explicitly** from `OneBlockPlugin.onDisable()` — nothing wires this automatically, so a new stateful manager that isn't added there silently loses its last flush window. Every manager that buffers writes is listed there today; if you add one, add it there in the same commit. `battle-pass` (`ProgressManager`, `BattlePassManager`) is the exception that needs nothing: it writes through to Mongo on every mutation instead of buffering.
- A dirty-set flush must **clear the set only after the write succeeds**, and go through the repository's bulk `updateAll`/`saveAll` rather than one `update` per entry. Clearing first turns any Mongo hiccup into silent data loss — `IslandManager` and `SellBeaconManager` both had that bug.

#### Storing `ItemStack`s

`ItemStack` is not POJO-codec-friendly — **persist it as a Base64 `String`**, never as a nested object. `ItemSerializer` (`lib/item-builder`, `one_block.libs.item_builder`) wraps `BukkitObjectOutputStream` + Base64 for this; the auction house is the reference usage (`Auction` holds the item as `String`). Serialize at the model boundary (when the item enters/leaves the stored object), not inside repository read/write.
- `XxxSystem.init` calls `XManager.onInit()`. Systems that need a fully loaded world (e.g. `IslandsSystem`) defer it with `runTaskLater`.

### Textura plugin (`plugins/textura`)

Separate jar, hard `depend` of one-block, own `plugin.yml` via `plugin-yml`. It builds and serves the server resource pack at runtime using **team.unnamed `creative`**:

Source packages: `pack/` (pack assembly), `registry/` + `model/` (config parsing), `api/` (the only surface other plugins touch), `text/` (`TextMetrics`, pixel width + spacing components), `glyph/`, `http/`, `hud/`, `block/`, `menu/`, `command/`, `listener/`, `dialog/`. There is no `util/` — new code goes in the package that owns the concern.

- Registries (`GlyphRegistry`, `FontRegistry`, `TitleFontRegistry`, `ItemRegistry`, `BlockRegistry`) load YAML from `server/plugins/Textura/configs/{glyphs,fonts,items,blocks}` — mutable, so `/textura reload` re-reads them in place. `BlockRegistry` additionally indexes `OVERRIDE`-type entries by target block. Config ymls are **flat** inside each folder, named `<domain>[-<sub>].yml`; the registries recurse but subfolders only create a second way to do the same thing.
- `ResourcePackBuilder` orchestrates and delegates per domain to `FontPackWriter` / `ItemPackWriter` / `BlockPackWriter`; `PackBuilder` is the only class that talks to the creative API, and `PackAssets` maps a declared `caminho` to a file on disk plus its position in the zip. The zip is assembled **asynchronously** (guarded by an `AtomicBoolean`) into `generated/resourcepack.zip`; `PackHash.calculateSHA1` produces the hash. Either a local `ResourcePackServer` (Ktor-less mini HTTP server on `host.porta`, default 8080) serves it, or it is uploaded to LobFile when `host.lobfile.enabled` is set — the two paths are exclusive and both end by setting `packUrl` + hash. The LobFile key lives in `host.lobfile.api-key`; `TEXTURA_LOBFILE_API_KEY` overrides it when set.
- The data folder splits by lifetime: `configs/` (definitions) and `resource/` (assets, laid out exactly as the pack — the file's location *is* its key) are versioned; `data/` holds runtime state that must survive (`block-allocations.yml` — losing it changes the appearance of blocks already placed in the world); `generated/` and `external/` are gitignored. `server/plugins/Textura/README.md` documents every yml format and the reload/gerar flow — read it before touching a config.
- Other plugins consume it only through the `api/` objects (`GlyphApi`, `HudApi`, `ItemApi`, `BlockApi`, `TitleApi`/`TitleBuilder`, `TexturaTags`), which hold a `@Volatile` reference to the live registry so reloads propagate. Do not reach into `registry/` from outside.
- `TitleApi` positions text by pixel inside GUI titles: each `y` becomes a generated `minecraft:font-ui-<y>` font in the pack. **A new `y` only exists after `/textura gerar`**, so register ascents early (`TitleApi.registerAscent(...)`, at system `init`) rather than the first time the menu opens. `TitleApi.source(path, baseAscent)` points a title at a custom font JSON (e.g. the digit-only `oneblock_level` font) so the `y` still means effective ascent.
- `TexturaTags` defines `<glyph:id>`, `<glyph:id:font>` and `<shift:n>`; both are self-closing and **never throw** — an unknown id renders empty rather than breaking the whole title. `Mini.mm` in `lib:formatter` is the MiniMessage instance carrying these resolvers.
- `dialog/` draws screens inside the vanilla dialog (design: `docs/superpowers/specs/2026-10-12-dialog-ui-json-design.md`): screens in `configs/ui/<id>.json`, themes in `configs/ui/*.yml`, art in `resource/assets/oneblock/textures/ui/`. `/textura gerar` writes the `oneblock:ui/*` fonts and publishes the `UiKit`; surfaces are pre-rendered per height (width is free at runtime) and sized icons per size, so changing a panel height or icon size needs a regenerate. Clicks travel as `ClickEvent.custom(textura:ui, "<token>:<index>")` handled by `DialogOpener` (no per-render callbacks). Other plugins open screens only through `DialogApi.open(player, id) { switch(...); segmented(...); text(...); button { click -> } }`. Screens also accept the Studio editor's keys (pages + `nav`, `onClick` actions `[open] [page] [set] [message] [console] [player] [sound]`, `dropdown`, `slider`, icon `states`, `{key?a:A|b:B}` / `%papi%` placeholders); control values are strings persisted per player in PDC (`textura:ui_<bind>`) unless code binds them, readable via `DialogApi.setting(player, key)`.

### Design docs

Feature designs written before implementation live in `docs/superpowers/specs/<date>-<feature>-design.md` (custom-item framework, miner minion, vacuum hopper, island settings, stackable spawners, glyph spritesheet, textura port, oneblock progression); the step-by-step implementation plans derived from them live in `docs/superpowers/plans/<date>-<feature>.md`. Check for an existing spec before designing a feature from scratch, and keep new specs/plans in those folders with the same naming. `.superpowers/sdd/` holds throwaway agent run artifacts — not documentation, and its `baseline-task` module is not in `settings.gradle.kts`.

## Conventions

- **Do not write comments in code.** No KDoc, no inline notes, no "why" blocks — not in Kotlin, not in Gradle scripts, not in YAML you generate. Name things well and let the code stand alone. This overrides any habit of explaining reasoning in-file: put the explanation in the chat response instead. Only exception: a comment the user explicitly asks for, or one that already exists and you are editing around.
- User-facing strings are in **Portuguese**; keep that when editing nearby code. Pre-existing comments are also in Portuguese — leave them as they are, don't translate or extend them.
- Commits follow `type(scope): description` (`feat`, `fix`, `refactor`, `docs`, `chore`) — see `git log`.
