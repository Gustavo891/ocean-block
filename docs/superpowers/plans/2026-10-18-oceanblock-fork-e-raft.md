# OceanBlock — Fork base + Oceano e Raft — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar o repositório `ocean-block` a partir do one-block (podado e renomeado) e entregar o primeiro jogável: jogador nasce num raft 3×3 num oceano vanilla sem estruturas, expande com qualquer bloco, e o reset restaura o mar.

**Architecture:** Cópia do diretório one-block (com `.git`) vira o fork; módulos descartados saem, o pacote `one_block` vira `ocean_block` (menos `one_block.textura`). O mundo principal `world` é gerado por um datapack que fixa a density function `overworld/continents` em oceano profundo. O sistema `islands` continua sendo o dono do lote; o bloco central e o avanço de fase saem, `StarterIsland` cola um schematic de raft e o reset usa regen do FAWE.

**Tech Stack:** Kotlin 2.3, JDK 25, Paper 26.3 (data pack format 121.0), Gradle multi-módulo com convention plugins, FAWE 2.15.1 (compileOnly), Kotest 5.9, MongoDB local.

**Spec:** `docs/superpowers/specs/2026-10-18-oceanblock-design.md` (até a Task 1, em `E:\Minecraft  System\docs\superpowers\specs\`).

## Global Constraints

- Repo do fork: `E:\Minecraft  System\ocean-block`. O diretório `E:\Minecraft  System\one-block` **nunca** é modificado (nem commit, nem arquivo).
- Sem comentários em código, Gradle ou YAML gerado (regra do `CLAUDE.md`).
- Strings para o jogador em português.
- Commits no formato `type(scope): descrição` (`feat`, `fix`, `refactor`, `docs`, `chore`), terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Toda task Gradle de build/deploy precisa conter `local` no nome (`localJar`, `localDeploy`); testes de módulos que tocam `lib:connection` rodam com `-Penv=local`.
- Servidor de dev **parado** antes de `localDeploy` (Windows trava o jar).
- Pacote novo: `ocean_block` (e `io.github.badnotice.ocean_block`). O pacote `one_block.textura` e os literais `"oneblock"` (namespaces, fontes `oneblock:*`, permissões `oneblock.*`) **não mudam**.
- Plugin: `name: ocean-block`, data folder `server/plugins/ocean-block/`, banco Mongo `ocean-block`, porta `25566`.
- Raft: bloco central em Y `62`, spawn em Y `63.0` com `+0.5` em X/Z, plataforma de fallback 3×3 de `OAK_PLANKS`, borda `30 → 50 → 70 → 100`.
- Datapack: density function `minecraft:overworld/continents` = constante `-0.6`; `generate-structures=false`; `allow-nether=false`.

## Review Focus

- **Schematic ausente ou corrompido** ao criar raft: jogador não pode nascer na água — fallback 3×3 + log `severe` (verificação manual na Task 7, footprint testado na Task 6).
- **Regen do FAWE falha** no reset: nada muda, jogador recebe mensagem de falha, entidades do lote não são removidas (Task 9; conferido no review, o FAWE não pode ser desligado em runtime).
- **Mundo `world` não carregado** ao criar raft: criação abortada antes de persistir qualquer coisa (Task 6, Step 5).
- **Datapack não carrega** (pack format errado): o mundo nasce com terra; a Task 5 para e reporta em vez de seguir (passo de `/datapack list` + `/locate biome minecraft:plains`).
- **Jogador entra pela primeira vez** depois do fork: `PlayerSpawnLocationEvent` herdado pode mandar para coordenada antiga (Y 100); o spawn salvo vem de `RaftPlacement.spawn` (Task 6) e é conferido no roteiro final (Task 10).
---

## Subprojeto 0 — Fork base

### Task 1: Copiar o one-block e fazer o commit de snapshot

**Files:**
- Create: `E:\Minecraft  System\ocean-block\` (cópia)
- Create: `ocean-block/docs/superpowers/specs/2026-10-18-oceanblock-design.md` (cópia da spec)
- Create: `ocean-block/docs/superpowers/plans/2026-10-18-oceanblock-fork-e-raft.md` (cópia deste plano)
- Modify: `ocean-block/.gitignore`

**Interfaces:**
- Produces: repositório git em `ocean-block` com o histórico do one-block até `58b2318` + commit de snapshot; remote `upstream` apontando para `../one-block`.

- [ ] **Step 1: Copiar o diretório sem artefatos de build e sem mundos**

Run (PowerShell):
```powershell
robocopy "E:\Minecraft  System\one-block" "E:\Minecraft  System\ocean-block" /E /XD build .gradle .kotlin "E:\Minecraft  System\one-block\server\world" "E:\Minecraft  System\one-block\server\world_nether" "E:\Minecraft  System\one-block\server\world_the_end" "E:\Minecraft  System\one-block\server\logs" /XF "hs_err_*.log" "replay_*.log"
```
Expected: exit code `< 8` (robocopy usa 0–7 para sucesso); `ocean-block\.git` existe.

- [ ] **Step 2: Ignorar `.kotlin/`**

Adicionar ao `.gitignore`: `.kotlin/`, `hs_err_*.log`, `replay_*.log`.

- [ ] **Step 3: Commit de snapshot**

```bash
cd "/e/Minecraft  System/ocean-block"
git add -A
git commit -m "chore: snapshot do one-block para o fork ocean-block"
```
Expected: `git status --short` vazio; `git merge-base --is-ancestor 58b2318 HEAD && echo ok` imprime `ok`.

- [ ] **Step 4: Remotes**

```bash
git remote remove origin 2>/dev/null; git remote add upstream "../one-block"; git remote -v
```
Expected: só `upstream`.

- [ ] **Step 5: Trazer spec e plano para o fork e commitar**

Copiar os dois arquivos de `E:\Minecraft  System\docs\superpowers\` para `ocean-block/docs/superpowers/{specs,plans}/`.
```bash
git add docs/superpowers
git commit -m "docs(oceanblock): spec e plano do fork base e raft"
```

### Task 2: Podar os módulos descartados

**Files:**
- Delete: `plugins/oneblock/system/{arena,combat-log,fly-timer,xp-bottle,minions,spawners,mob-drops,repair,special-items,chats}/`
- Delete: `plugins/oneblock/system/custom-items/src/main/kotlin/one_block/system/custom_items/content/{magnet,vacuum_hopper}/`
- Modify: `settings.gradle.kts`
- Modify: `plugins/oneblock/plugin/src/main/kotlin/io/github/badnotice/one_block/OneBlockPlugin.kt`
- Modify: `plugins/oneblock/plugin/build.gradle.kts` (`softDepend`)
- Modify: `plugins/oneblock/system/trader/build.gradle.kts`, `.../trader/manager/ItemsRegistry.kt`
- Modify: `plugins/oneblock/system/custom-items/build.gradle.kts`, `.../custom_items/CustomItemSystem.kt`
- Delete: `server/plugins/one-block/mob-drops.yml`

**Interfaces:**
- Produces: build com só os módulos mantidos (spec 1.2).

- [ ] **Step 1: Remover os módulos do build e do disco**

`git rm -r` nas 10 pastas. Em `settings.gradle.kts`, tirar as 10 entradas `":plugins:oneblock:system:<nome>"` e a linha final duplicada `include("plugins:oneblock:system:spawners")`.

- [ ] **Step 2: Desligar no plugin**

Em `OneBlockPlugin.kt`, remover imports e chamadas de: `ArenaSystem` (init e `setupCommands`), `SpawnersSystem` (init e `onDisable`), `CombatLogSystem`, `FlyTimerSystem`, `FlyPlayerManager.onDisable()`, `MinionsSystem`, `PlacedMinionManager.onDisable()`, `MobDropsSystem`, `RepairsSystem`, `SpecialItemSystem`, `SellBeaconManager.onDisable()`, `ItemCommand` (de `special_items`), `XpBottleSystem`. Em `plugin/build.gradle.kts`, `softDepend = listOf("packetevents")`.

- [ ] **Step 3: Desacoplar `trader`**

Remover a entrada `TraderItem("tempo_voo", ...)` e o import `one_block.system.fly_timer.Flys` de `ItemsRegistry.kt`; tirar `projects.plugins.oneblock.system.flyTimer` do `trader/build.gradle.kts`.

- [ ] **Step 4: Desacoplar `custom-items`**

Apagar `content/magnet` e `content/vacuum_hopper`; em `CustomItemSystem` remover `CustomItemRegistry.register(MagnetItem())` e o registro do vacuum hopper em `registerBuiltInBlocks`; tirar `projects.plugins.oneblock.system.minions` do `custom-items/build.gradle.kts`.

- [ ] **Step 5: Garantir que nada aponta para módulo removido**

Run:
```bash
grep -rnE "system\.(arena|combatLog|flyTimer|xpBottle|minions|spawners|mobDrops|repair|specialItems|chats)\b" --include=*.kts plugins lib
grep -rnE "one_block\.system\.(arena|combatlog|fly_timer|xpbottle|minions|spawners|mobdrops|repair|special_items|chats)\b" --include=*.kt plugins lib
```
Expected: as duas saídas vazias. Qualquer resto: cortar o uso no módulo mantido (spec 3.3), nunca reintroduzir o módulo.

- [ ] **Step 6: Build**

Run: `./gradlew :plugins:oneblock:plugin:localJar`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(oceanblock): remove módulos fora do escopo do OceanBlock"
```

### Task 3: Renomear para ocean-block

**Files:**
- Move: todo `src/*/kotlin/one_block/` em `lib/**` e `plugins/oneblock/**` → `.../ocean_block/`; `plugins/oneblock/plugin/src/main/kotlin/io/github/badnotice/one_block/` → `.../ocean_block/`
- Move: `plugins/oneblock/` → `plugins/oceanblock/`
- Move: `OneBlockPlugin.kt` → `OceanBlockPlugin.kt`, `command/OneBlockCommand.kt` → `command/OceanBlockCommand.kt`
- Modify: todos os `*.kt` / `*.kts` de `lib/`, `plugins/oceanblock/`, `build-logic/`, `settings.gradle.kts`
- Modify: `lib/connection/src/{local,production}/kotlin/ocean_block/libs/connection/Mongo.kt`
- Modify: `CLAUDE.md`

**Interfaces:**
- Produces: projeto Gradle `:plugins:oceanblock:plugin`, classe `io.github.badnotice.ocean_block.OceanBlockPlugin`, comando `/oceanblock`, jar `ocean-block.jar`.

- [ ] **Step 1: Guarda — Textura não depende de `libs`/`system`**

Run: `grep -rn "one_block\.\(libs\|system\)" plugins/textura/src`
Expected: vazio (se não for, parar e reportar: a renomeação quebraria o Textura).

- [ ] **Step 2: Mover os diretórios de pacote**

```bash
find lib plugins/oneblock -type d -name one_block -path "*/kotlin/*" -not -path "*/build/*" | while read d; do git mv "$d" "$(dirname "$d")/ocean_block"; done
```
Expected: `find lib plugins/oneblock -type d -name one_block -path "*/kotlin/*"` vazio.

- [ ] **Step 3: Reescrever pacotes e imports**

```bash
git ls-files lib plugins/oneblock build-logic | grep -E '\.(kt|kts)$' | xargs perl -pi -e 's/\bone_block\b(?!\.textura)/ocean_block/g'
```

- [ ] **Step 4: Renomear o caminho Gradle**

`git mv plugins/oneblock plugins/oceanblock`, depois:
```bash
git ls-files '*.kts' CLAUDE.md | xargs perl -pi -e 's/plugins([.:\/])oneblock/plugins${1}oceanblock/g'
```
`settings.gradle.kts`: `rootProject.name = "ocean-block"`.

- [ ] **Step 5: Plugin, comando e banco**

- `OceanBlockPlugin` (classe e arquivo), referências `this@OceanBlockPlugin`.
- `OceanBlockCommand` com `@CommandAlias("oceanblock")`.
- `plugin/build.gradle.kts`: `bukkit { name = "ocean-block"; main = "io.github.badnotice.ocean_block.OceanBlockPlugin" }`, `archiveBaseName.set("ocean-block")`.
- Os dois `Mongo.kt`: banco `"ocean-block"`.

- [ ] **Step 6: Verificar renomeação, build e testes**

Run:
```bash
grep -rn "one_block" --include=*.kt --include=*.kts lib plugins/oceanblock build-logic | grep -v "one_block\.textura"
./gradlew :plugins:oceanblock:plugin:localJar
./gradlew :plugins:oceanblock:system:islands:test :plugins:oceanblock:system:custom-items:test -Penv=local
```
Expected: grep vazio; `BUILD SUCCESSFUL` nos dois Gradle; `build/libs/ocean-block.jar` existe em `plugins/oceanblock/plugin`.

- [ ] **Step 7: Atualizar `CLAUDE.md`**

Nomes e caminhos novos (`ocean-block`, `:plugins:oceanblock:`, `OceanBlockPlugin`, data folder `server/plugins/ocean-block/`); remover os trechos dos módulos podados (minions/ModelEngine, special-items, chats, spawners); adicionar no topo uma linha apontando para a spec do OceanBlock e a regra de rename (`one_block.textura` e literais `"oneblock"` preservados).

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(oceanblock): renomeia projeto, pacotes e plugin para ocean-block"
```

### Task 4: Servidor de desenvolvimento

**Files:**
- Modify: `.gitignore`
- Modify: `server/server.properties`
- Move: `server/plugins/one-block/` → `server/plugins/ocean-block/`
- Delete: `server/plugins/one-block.jar`

**Interfaces:**
- Produces: servidor de dev que sobe o `ocean-block.jar` na porta `25566`.

- [ ] **Step 1: Parar de versionar runtime do servidor**

Adicionar ao `.gitignore`:
```
server/world/*
!server/world/datapacks/
server/world_nether/
server/world_the_end/
server/spawn/
server/logs/
server/cache/
server/libraries/
server/versions/
server/.console_history
```
Run: `git rm -r --cached --quiet server/world server/world_nether server/world_the_end server/spawn server/logs server/cache server/libraries server/versions server/.console_history 2>/dev/null; git ls-files 'server/**/hs_err_*.log' | xargs -r git rm --cached`

- [ ] **Step 2: Configs**

`server.properties`: `server-port=25566`. `git mv server/plugins/one-block server/plugins/ocean-block`; `git rm server/plugins/one-block.jar`.

- [ ] **Step 3: Deploy e boot**

Run: `./gradlew :plugins:oceanblock:plugin:localDeploy`, depois `server/start.bat` (MongoDB local rodando).
Expected no log: `Enabling ocean-block`, nenhuma `Exception`/`SEVERE` com `ocean_block` no stacktrace, nenhum `Could not load` de plugin.

- [ ] **Step 4: Smoke dos comandos mantidos**

No console/jogo: `/oceanblock reload`, `/island`, `/shop`, `/sell`, `/tops` respondem. Parar o servidor.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore(server): servidor de dev do ocean-block na porta 25566"
```

---

## Subprojeto 1 — Oceano + lote do raft

### Task 5: Datapack do oceano (spike validado e mantido)

**Files:**
- Create: `server/world/datapacks/oceanblock-world/pack.mcmeta`
- Create: `server/world/datapacks/oceanblock-world/data/minecraft/worldgen/density_function/overworld/continents.json`
- Modify: `server/server.properties`

**Interfaces:**
- Produces: mundo `world` só oceano, sem estruturas. Tasks 6–10 assumem nível do mar 63.

- [ ] **Step 1: Apagar mundos gerados no boot da Task 4**

Com o servidor parado: apagar `server/world` (exceto `datapacks/`), `server/world_nether`, `server/world_the_end`, `server/spawn`.

- [ ] **Step 2: Escrever o datapack**

`pack.mcmeta`:
```json
{ "pack": { "description": "OceanBlock: mundo so oceano", "min_format": [121, 0], "max_format": [121, 0] } }
```
`continents.json`:
```json
{ "type": "minecraft:constant", "argument": -0.6 }
```
`server.properties`: `generate-structures=false`, `allow-nether=false`.

- [ ] **Step 3: Boot e validação do mundo**

Subir o servidor. No jogo, como op:
- `/datapack list` → `file/oceanblock-world` em *enabled*.
- `/locate biome minecraft:plains` → não encontrado.
- `/locate biome minecraft:warm_ocean` e `/locate biome minecraft:frozen_ocean` → encontrados.
- `/locate structure minecraft:shipwreck` → não encontrado.
- Voar ~2000 blocos em duas direções: só água; mergulhar em (0, ~, 0): fundo de oceano profundo com areia/cascalho.

Se aparecer terra ou o datapack não estiver *enabled*: **parar a task e reportar** (alternativa da spec 4.2: `noise_settings` próprio). Não improvisar outra técnica.

- [ ] **Step 4: Validar regen do FAWE**

Em X=5000, Z=5000: colocar blocos na superfície e minerar o fundo; selecionar 100×100 da altura mínima à máxima (`//pos1`, `//pos2`) e rodar `//regen`.
Expected: água e fundo vanilla restaurados. Se falhar: **parar e reportar** (alternativa da spec 4.2: snapshot do lote).

- [ ] **Step 5: Commit**

```bash
git add server/world/datapacks server/server.properties
git commit -m "feat(worlds): datapack de mundo so oceano sem estruturas"
```

### Task 6: Posicionamento do raft (TDD)

**Files:**
- Create: `plugins/oceanblock/system/islands/src/main/kotlin/ocean_block/system/islands/island/RaftPlacement.kt`
- Test: `plugins/oceanblock/system/islands/src/test/kotlin/ocean_block/system/islands/island/RaftPlacementTest.kt`
- Modify: `plugins/oceanblock/system/islands/build.gradle.kts` (`testImplementation(projects.lib.coordinate)`)
- Modify: `.../island/IslandManager.kt` (`createIsland`: `center` e `spawn`)
- Modify: `.../reset/IslandResetService.kt` (`resetProgress`: `island.spawn`)

**Interfaces:**
- Produces:
```kotlin
object RaftPlacement {
    const val SURFACE_Y = 62
    const val FALLBACK_SIZE = 3
    fun center(position: Position, world: String): SimpleLocation
    fun spawn(position: Position, world: String): SimpleLocation
    fun fallbackFootprint(centerX: Int, centerZ: Int): List<Pair<Int, Int>>
}
```

- [ ] **Step 1: Escrever os testes**

```kotlin
class RaftPlacementTest : StringSpec({
    "centro do raft fica na superficie do mar" {
        val position = Position.fromIndex(1)
        val center = RaftPlacement.center(position, "world")
        center.x shouldBe position.originX
        center.y shouldBe 62.0
        center.z shouldBe position.originZ
        center.world shouldBe "world"
    }

    "spawn fica em cima do bloco central" {
        val position = Position.fromIndex(1)
        val spawn = RaftPlacement.spawn(position, "world")
        spawn.x shouldBe position.originX + 0.5
        spawn.y shouldBe 63.0
        spawn.z shouldBe position.originZ + 0.5
    }

    "fallback cobre 3x3 em volta do centro" {
        val footprint = RaftPlacement.fallbackFootprint(10, 20)
        footprint.size shouldBe 9
        footprint.toSet() shouldBe (-1..1).flatMap { dx -> (-1..1).map { dz -> (10 + dx) to (20 + dz) } }.toSet()
    }
})
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew :plugins:oceanblock:system:islands:test --tests "*RaftPlacement*" -Penv=local`
Expected: FAIL, `Unresolved reference: RaftPlacement`.

- [ ] **Step 3: Implementar `RaftPlacement`**

`fallbackFootprint` usa `FALLBACK_SIZE / 2` como raio.

- [ ] **Step 4: Rodar e ver passar**

Mesmo comando. Expected: PASS (3 testes) e `IslandSettingsTest` continua passando.

- [ ] **Step 5: Usar no lote**

`IslandManager.createIsland`: `center = RaftPlacement.center(grid, "world")`, `spawn = RaftPlacement.spawn(grid, "world")`; antes de indexar, se `Bukkit.getWorld("world") == null`, logar `severe` `"[islands] Criação de ilha para ${player.name} recusada: mundo world não carregado."` e retornar `null` (nada persistido). `IslandResetService.resetProgress`: `island.spawn = RaftPlacement.spawn(island.position, island.center.world)`.
Run: `./gradlew :plugins:oceanblock:plugin:localJar` → `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(islands): posiciona o lote do raft na superficie do mar"
```

### Task 7: Raft inicial (schematic + fallback)

**Files:**
- Modify: `.../island/StarterIsland.kt`
- Create: `server/plugins/ocean-block/templates/raft.schem`
- Delete: `server/plugins/ocean-block/templates/forest.schem`

**Interfaces:**
- Consumes: `RaftPlacement.SURFACE_Y`, `RaftPlacement.fallbackFootprint` (Task 6).
- Produces: `StarterIsland.generate(island: Island)` (mesma assinatura, usado por `IslandManager` e `IslandResetService`) e `StarterIsland.placeFallback(island: Island)`.

- [ ] **Step 1: Criar o schematic no servidor de dev**

Em qualquer ponto do oceano: 3×3 de `OAK_PLANKS` em Y 62, um baú em cima da tábua do canto (+1, 63, +1). `//pos1` em (-1, 62, -1) e `//pos2` em (+1, 63, +1) relativos ao centro; em pé em cima da tábua central (pés em Y 63), `//copy` e `//schem save raft`. Copiar `plugins/FastAsyncWorldEdit/schematics/raft.schem` para `server/plugins/ocean-block/templates/raft.schem`; apagar `forest.schem`.

- [ ] **Step 2: Reescrever `StarterIsland`**

- `SCHEMATIC = "raft.schem"`, `PASTE_Y_OFFSET = 1` (origem do `//copy` fica um bloco acima da tábua central).
- Remover `BIOME`, `BIOME_STEP`, `biome`, o laço de `setBiome` e `placeFloor` (e a chamada final a ele).
- Paste com `.ignoreAirBlocks(true)`.
- Schematic inexistente ou falha no paste → `placeFallback(island)` na thread principal + `Instance.logger.severe("[raft] ...")` com o uuid da ilha. Mundo não carregado → só o log `severe` (a Task 6 já impede criar raft nesse caso).
- `placeFallback`: `OAK_PLANKS` em cada `(x, z)` de `RaftPlacement.fallbackFootprint(center.blockX, center.blockZ)` em `RaftPlacement.SURFACE_Y`.

- [ ] **Step 3: Build, deploy e verificação manual**

Run: `./gradlew :plugins:oceanblock:plugin:localDeploy`, subir servidor.
- Jogador novo, `/island` → mensagem de criação, jogador cai em cima do raft 3×3 com baú, rente à água.
- Parar, renomear `raft.schem` para `raft.schem.bak`, subir, outro jogador novo `/island` → plataforma 3×3 de tábuas + log `[raft]` severe. Voltar o nome do arquivo.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(islands): raft inicial por schematic com fallback 3x3"
```

### Task 8: Remover o bloco central e o avanço de fase

**Files:**
- Delete: `.../phase/PhaseBlocks.kt`, `.../phase/PhaseAnimation.kt`, `.../phase/PhaseAdvancement.kt`, `.../phase/PhaseProgress.kt`, `.../util/MonsterParty.kt`
- Modify: `.../island/Island.kt`, `.../island/IslandManager.kt`, `.../listener/PlayerListener.kt`, `.../reset/IslandResetService.kt`, `.../command/IslandCommand.kt`, `.../hud/IslandHud.kt`, `.../expansion/IslandExpansion.kt`
- Modify: `plugins/oceanblock/plugin/src/main/kotlin/io/github/badnotice/ocean_block/OceanBlockPlugin.kt` (completion `"phases"`)

**Interfaces:**
- Produces: `Island` sem `grid`, `processBlockBreak`, `advancePhase`, `handleMobSpawn`, `handleMonsterParty`, `chance`. Mantidos e dormentes: `Phase`, `PhaseManager`, `PhaseRewarder`, `PhaseView`, `PhaseDetailView`, `MobSpawnData` (constantes), `monster/*`.

- [ ] **Step 1: Apagar os arquivos e limpar `Island`**

Remover os cinco arquivos e, em `Island`, o campo `grid` e as funções listadas em Interfaces, com seus imports.

- [ ] **Step 2: Cortar os chamadores**

- `PlayerListener`: remover o handler `onBlockBreak` inteiro e `contentsOf`, com os imports que só eles usavam (`PhaseAdvancement`, `BlockApi`, `Container`, ...).
- `IslandManager.migrateUnknownPhases`: remover `island.grid = 0`.
- `IslandResetService`: em `resetProgress` remover `PhaseAdvancement.clear` e `island.grid = 0`; no preview, tirar `(grid ...)`; `restoreCenter` passa a ser só `StarterIsland.generate(island)`.
- `IslandCommand`: remover subcomandos `phases|fases` e `setphase|setfase`.
- `OceanBlockPlugin.setupCommands`: remover a completion `"phases"`.
- `IslandHud.contentFor`: `HudContent(text = "<white>Raft <dark_gray>• <gray>${island.size}x${island.size}", progress = 1f)`, sem depender de fase.
- `IslandExpansion`: remover o ramo `"phaseprogress"`.

- [ ] **Step 3: Verificar**

Run:
```bash
grep -rnE "PhaseBlocks|PhaseAnimation|PhaseAdvancement|PhaseProgress|MonsterParty|processBlockBreak|\.grid\b" --include=*.kt plugins/oceanblock
./gradlew :plugins:oceanblock:plugin:localJar
./gradlew :plugins:oceanblock:system:islands:test -Penv=local
```
Expected: grep vazio; `BUILD SUCCESSFUL`; testes PASS.

- [ ] **Step 4: Verificação manual**

Deploy + boot. No raft: quebrar a tábua central → dropa `OAK_PLANKS`, bloco não volta, nenhum mob nasce; HUD mostra `Raft • 30x30`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(islands): remove bloco central e avanco de fase do one-block"
```

### Task 9: Reset que restaura o mar

**Files:**
- Create: `.../reset/OceanRegen.kt`
- Modify: `.../reset/IslandResetService.kt` (`resetTerrain`)
- Delete: `.../reset/TerrainCleaner.kt`

**Interfaces:**
- Consumes: `IslandArea` (existente), `StarterIsland.generate` (Task 7).
- Produces: `object OceanRegen { fun regenerate(area: IslandArea, onDone: (Boolean) -> Unit) }` — `onDone` sempre chamado na thread principal, `true` só se o regen do WorldEdit retornar `true` sem exceção.

- [ ] **Step 1: Implementar `OceanRegen`**

Corpo (a escolha da API não sai da assinatura):
```kotlin
Bukkit.getScheduler().runTaskAsynchronously(Instance, Runnable {
    val ok = runCatching {
        val weWorld = BukkitAdapter.adapt(area.world)
        val region = CuboidRegion(
            weWorld,
            BlockVector3.at(area.minX, area.minY, area.minZ),
            BlockVector3.at(area.maxX, area.maxY, area.maxZ),
        )
        WorldEdit.getInstance().newEditSession(weWorld).use { weWorld.regenerate(region, it) }
    }.onFailure {
        Instance.logger.severe("[raft] Falha no regen da area ${area.minX},${area.minZ}: ${it.message}")
    }.getOrDefault(false)

    Bukkit.getScheduler().runTask(Instance, Runnable { onDone(ok) })
})
```

- [ ] **Step 2: Reescrever `resetTerrain`**

Ordem: valida área e `running` (como hoje) → `IslandResetHooks.fireTerrainReset(island, area)` → mensagem `"<gray>Restaurando o mar do raft de <white>${island.ownerName}</white>..."` → `OceanRegen.regenerate(area) { ok -> ... }`. No callback: `running.remove(island.uuid)`; se `!ok`, `sender.sendRichMessage("<red>Falha ao restaurar o mar do raft de <white>${island.ownerName}</white>. Nada foi alterado.")` e retorna; se `ok`, remove entidades não-jogador da área, `StarterIsland.generate(island)`, `returnPlayers(island, area)`, `"<green>Raft de <white>${island.ownerName}</white> resetado."`. Apagar `TerrainCleaner.kt` e `restoreCenter`.

- [ ] **Step 3: Build e verificação manual**

Run: `./gradlew :plugins:oceanblock:plugin:localDeploy`, boot.
- No próprio raft: construir para fora, minerar o fundo, soltar um item e um mob; `/island reset terrain <dono> -confirm` → mar e fundo restaurados, raft recolado, mob/item removidos, jogador teleportado para o raft.
- Ramo de falha (o FAWE é `depend`, não dá para tirá-lo em runtime): conferido no review — com `ok = false` o callback retorna antes de remover entidades ou colar o raft, e `running` é liberado.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(islands): reset do raft restaura o oceano via regen do FAWE"
```

### Task 10: Nome "Raft" para o jogador e roteiro final

**Files:**
- Modify: `.../command/IslandCommand.kt` (`@CommandAlias`)
- Modify: strings de jogador com "ilha" em `plugins/oceanblock/system/**/src/main/kotlin` (mensagens, títulos e lore de menus, textos de placeholder)

**Interfaces:**
- Produces: comando `/raft` com aliases `/balsa`, `/ilha`, `/island`, `/is`.

- [ ] **Step 1: Alias**

`@CommandAlias("raft|balsa|ilha|island|is")`.

- [ ] **Step 2: Trocar "ilha" por "raft" nas strings de jogador**

Run: `grep -rn -i "ilha" --include=*.kt plugins/oceanblock/system`. Em cada string mostrada ao jogador, trocar ajustando o gênero ("sua ilha" → "seu raft", "da ilha" → "do raft", "nessa ilha" → "nesse raft", "ilhas" → "rafts"). Nomes de classe, chaves de config e mensagens de log com prefixo `[ilha]`/`[islands]` ficam.
Expected depois: o grep só retorna nomes de classe/identificadores, logs e o alias.

- [ ] **Step 3: Build + roteiro de aceitação (spec 4.11)**

Run: `./gradlew :plugins:oceanblock:plugin:localDeploy`, boot. Com dois jogadores novos:
1. `/raft` cria o lote; jogador aparece em cima do raft rente à água; mensagens dizem "raft".
2. Constrói para fora com terra, pedregulho e tábua a partir da borda; fora da borda não consegue.
3. Upgrade de borda no menu: borda vai de 30 para 50.
4. `/raft member <outro>`: o outro constrói; sem membro/trust, não constrói.
5. `/raft reset terrain <dono> -confirm`: mar restaurado, raft recolado.
6. Sair e entrar de novo: volta para o raft (spawn Y 63).
7. `/locate structure minecraft:shipwreck` não encontra; mundo `spawn` carrega.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(islands): comando /raft e textos de raft para o jogador"
```
