# Raid de piratas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task (o usuário pediu tudo na thread principal, sem subagentes). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Raid de piratas aleatória por ilha (1 por dia, 3% a cada 10 min, nível 10+), 3 ondas em barcos vanilla, Super Pirata dourado na última, HUD de invasão no lugar da missão, moedas/pérolas físicas e bônus de vitória.

**Architecture:** Módulo novo `plugins/oceanblock/system/pirate-raid` (`ocean_block.system.pirate_raid`, `PirateRaidSystem.init(plugin, paperCommandManager)`). Regras puras (config, sorteio, ondas, drops, valor, rota do barco, máquina de estados, textos) cobertas por Kotest; a parte Bukkit (barcos, mobs, pickup, HUD, toasts) só pelo checklist manual. `missions` ganha `MissionHud.suspend/resume`.

**Tech Stack:** Kotlin 2.3, Paper 26.3, Kotest 5.9, MongoDB (`lib:connection`), Textura (`HudApi`, `NotificationApi`, `ItemApi`).

**Spec:** `docs/superpowers/specs/2026-10-27-oceanblock-raid-piratas-design.md`

## Global Constraints

- Código sem comentários; textos para o jogador em pt-BR.
- Commits `type(scope): descrição` terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; commitar só arquivos desta tarefa.
- Gradle sempre por tarefa com `local` no nome (`localTest`, `localDeploy`).
- Nunca parar/reiniciar o servidor sem o sim do usuário.
- Arte original (sem copiar assets de mods).
- Valores padrão da spec: chance 0.03, intervalo 12000 ticks, nível mínimo 10, duração 300 s, 3 ondas, raio 32 (mínimo 16), velocidade 0.25, desembarque 8, limite 64, ilha vazia 30 s, moeda 150–400, fator `1 + 0.05 × (nível - 10)`, vitória 3000 coins e 0.2 × `toNext`, Super Pirata vida 80.

## Review Focus

- Pirata morto por qualquer causa (lava, afogado, `/kill`, void) deve contar e avançar a onda: o contador vem de `isValid/isDead` checado a cada tick, não só do `EntityDeathEvent`.
- Chunk do pirata descarregado: entidade some sem morrer; o tick trata `!isValid` como morta, sem drops.
- Moeda pega por não membro, por mob (pillager pega itens) ou por funil: cancelado, item fica.
- Dois membros na ilha no mesmo sorteio: uma raid só por ilha (o sorteio agrupa por ilha).
- `/piratas iniciar` numa ilha com raid ativa: recusa com mensagem.

---

### Task 1: Regras puras + testes

**Files (create, em `plugins/oceanblock/system/pirate-raid/`):**
- `build.gradle.kts` (como `playtime-rewards`, + `islands`, `missions`, `drift`, `lib:coordinate`; `localTest` alias)
- `src/main/kotlin/ocean_block/system/pirate_raid/config/RaidConfig.kt`, `RaidConfigParser.kt`
- `.../rules/RaidRoll.kt`, `WaveTable.kt`, `LootRoller.kt`, `CoinValue.kt`, `BoatPath.kt`, `RaidState.kt`
- `.../text/RaidHudText.kt`, `.../notify/RaidCard.kt`
- `src/main/resources/piratas.yml`
- Testes espelhados em `src/test/kotlin/...`
- Modify: `settings.gradle.kts` (+ `:plugins:oceanblock:system:pirate-raid`)

**Interfaces (Produces):**
- `data class Band(minLevel: Int, waves: List<Int>, bruteShare: Double)`; `data class LootEntry(item: String, chance: Double, min: Int, max: Int)` (`item` = material ou `moeda`/`perola`); `data class SuperPirateConfig(health: Double, coins: Int, rolls: Int)`; `data class RaidConfig(chance, intervalTicks, minLevel, durationSeconds, emptySeconds, boatDistance, boatMinDistance, boatSpeed, landingDistance, leashDistance, coinMin, coinMax, levelFactor, victoryCoins, victoryXpFraction, bands: List<Band>, drops: List<LootEntry>, superPirate)` com `RaidConfig.DEFAULT`.
- `RaidConfigParser.parse(config: ConfigurationSection, isMaterial: (String) -> Boolean, warn: (String) -> Unit): RaidConfig`.
- `RaidRoll.eligible(active: Boolean, lastDay: String?, today: String, maxLevel: Int, minLevel: Int): Boolean`; `RaidRoll.hits(roll: Double, chance: Double): Boolean`.
- `WaveTable.band(bands, level): Band`; `data class Crew(pillagers: Int, brutes: Int, superPirate: Boolean)`; `WaveTable.crew(band, wave: Int /*1-based*/, waves: Int, random: Random): Crew`; `WaveTable.boats(crew): Int` = `ceil(total/2)` com o Super Pirata num barco próprio.
- `data class Drop(item: String, amount: Int)`; `LootRoller.roll(entries, rolls: Int, random): List<Drop>` (moeda rola uma vez só, independente de `rolls`).
- `CoinValue.factor(level, levelFactor): Double`; `CoinValue.coin(min, max, level, levelFactor, random): Int`; `CoinValue.victory(base, level, levelFactor): Int`.
- `data class Vec(x: Double, z: Double)`; `BoatPath.spawnPoint(center: Vec, radius, angle): Vec`; `BoatPath.step(position: Vec, center: Vec, speed): Vec` (velocidade, nunca passa do centro); `BoatPath.landed(distance, landingDistance, blockedAhead, stillTicks): Boolean` (`stillTicks >= 60`).
- `sealed RaidPhase { Wave(n), Won, Fled, Abandoned }`; `RaidState.next(wave: Int, waves: Int, alive: Int, elapsedTicks: Long, durationTicks: Long, emptyTicks: Long, emptyLimit: Long): RaidPhase`.
- `RaidHudText.line(wave, waves, alive, secondsLeft): String` → `Invasão pirata · Onda 2/3 · 5 piratas · 3:12` (1 pirata no singular).
- `data class ToastLine(text, icon)`; `RaidCard.started()`, `RaidCard.won(coins: Int)` (`+N Coins` com `NumberFormatter` do formatter → teste usa `2.000`), `RaidCard.fled()`.

- [ ] **Step 1:** testes Kotest (falham): parser com valores ruins cai no padrão e loga `[piratas]`; `eligible` falso com raid ativa / mesmo dia / nível 9; faixa 25 → faixa 20; `crew(onda 3)` traz `superPirate = true`, ondas 1–2 não; brutamontes `round(total × share)`; `roll` com `Random(seed)` determinístico, chance 0 nunca, 1 sempre, moeda única com `rolls = 3`; `factor(50, 0.05) == 3.0`, `factor(10) == 1.0`; `step` não ultrapassa o centro; `landed` por distância, bloqueio e 60 ticks parado; `next` → `Won` quando onda 3 e 0 vivos, `Wave(2)` quando onda 1 e 0 vivos, `Fled` com tempo esgotado e vivos, `Abandoned` com vazio ≥ limite; texto do HUD com `0:05` e singular.
- [ ] **Step 2:** `./gradlew :plugins:oceanblock:system:pirate-raid:localTest` → FAIL (não compila).
- [ ] **Step 3:** implementar as regras e o `piratas.yml` padrão.
- [ ] **Step 4:** `localTest` → PASS.
- [ ] **Step 5:** commit `feat(piratas): regras puras da raid de piratas`.

### Task 2: Suspender HUD de missão

**Files:** Modify `plugins/oceanblock/system/missions/src/main/kotlin/ocean_block/system/missions/hud/MissionHud.kt`.

**Produces:** `MissionHud.suspend(player: Player)` (marca uuid, esconde slot `missao`), `MissionHud.resume(player: Player)` (desmarca, `show` com `MissionManager.get`); `show` esconde e retorna enquanto suspenso; `hide` também desmarca.

- [ ] **Step 1:** implementar; `./gradlew :plugins:oceanblock:system:missions:localTest` → PASS.
- [ ] **Step 2:** commit `feat(missoes): HUD de missão pode ser suspenso`.

### Task 3: Runtime Bukkit, arte e integração

**Files (create em `ocean_block.system.pirate_raid`):**
- `config/RaidFile.kt` (copia/carrega `piratas.yml`).
- `data/PirateRaidDay.kt`, `PirateRaidRepository.kt` (coleção `pirate_raids`, índice único `islandUuid`), `RaidDays.kt` (mapa em memória, `lastDay(island)`, `mark(island, day)` write-through no `ScheduledExecutor`).
- `crew/PirateKeys.kt` (PDC `oceanblock:pirata` ilha, `oceanblock:super_pirata`, `oceanblock:raid_moeda`, `oceanblock:raid_perola`, `oceanblock:raid_ilha`), `PirateTeams.kt` (times `oceanblock_piratas` RED e `oceanblock_superpirata` GOLD no scoreboard principal), `PirateFactory.kt` (Pillager/Vindicator/Super Pirata com nome, equipamento, glow, time).
- `raid/ActiveRaid.kt` (ilha, nível, onda, piratas vivos, barcos, ticks), `BoatPilot.kt` (DarkOakBoat, passa velocidade de `BoatPath.step`, desembarque), `RaidDirector.kt` (sorteio periódico, `start`, tick a cada 1 tick para barcos e 20 ticks para estado/HUD, `end`), `RaidHud.kt` (slot `invasao` + `MissionHud.suspend/resume` por membro no lote).
- `loot/RaidLoot.kt` (spawna drops, `ItemApi.create("moeda_pirata")` com PDC, despawn 2 min), `listener/PirateListener.kt` (morte: limpa drops, rola loot; `EntityChangeBlockEvent`, `EntityBreakDoorEvent` cancelados; `EntityTargetEvent` mantém alvo em jogador), `listener/LootPickupListener.kt` (`EntityPickupItemEvent`, `InventoryPickupItemEvent`; deposita via `Currencies.deposit`, avisa via `CollectNotifier.notify`).
- `notify/RaidNotifier.kt` (toasts `piratas_0/1` na região `toasts`, corneta `EVENT_RAID_HORN`, `ENTITY_PLAYER_LEVELUP` na vitória).
- `command/PiratasCommand.kt` (`/piratas iniciar [jogador]`, `/piratas parar`, `oneblock.admin`).
- `PirateRaidSystem.kt` (`init`, `onDisable` remove piratas, barcos e itens de raid).
- Textura: `configs/items/piratas.yml` (`moeda_pirata`, `perola`), `configs/glyphs/piratas.yml` (`toast_pirata` 145×26 ascent 7, `moeda_pirata_icone`, `perola_icone` 16 px ascent 8), PNGs em `resource/assets/oneblock/textures/item/piratas/` e `textures/icons/piratas/` desenhados por script descartável no scratchpad.
- Modify: `OceanBlockPlugin.kt` (`PirateRaidSystem.init` depois de `PlaytimeRewardsSystem`, `onDisable`), `plugins/oceanblock/plugin/build.gradle.kts` só se o loop não incluir sozinho.

- [ ] **Step 1:** desenhar a arte (moeda, pérola, selo do toast) e registrar ymls.
- [ ] **Step 2:** implementar runtime.
- [ ] **Step 3:** `./gradlew :plugins:oceanblock:plugin:localJar` → BUILD SUCCESSFUL.
- [ ] **Step 4:** commit `feat(piratas): raid de piratas nas ilhas`.

### Task 4: Docs e teste no jogo

- [ ] **Step 1:** seção `#### Pirate raid (system/pirate-raid)` no `CLAUDE.md` e lista de módulos com testes; seção nova no checklist manual `docs/superpowers/plans/2026-10-18-oceanblock-aceite-manual.md`.
- [ ] **Step 2:** commit `docs(piratas): CLAUDE.md e checklist da raid`.
- [ ] **Step 3:** pedir ao usuário para parar o servidor, `localDeploy` dos dois plugins, `start`, `/textura gerar`, `/piratas iniciar` e conferir: barcos chegam, brilho vermelho/dourado, HUD troca a missão, moeda vira coins, vitória/fuga com toast.
