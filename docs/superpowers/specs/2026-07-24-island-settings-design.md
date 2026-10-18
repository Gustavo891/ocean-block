# Configurações de ilha (island settings)

Data: 2026-07-24
Módulo: `plugins/oneblock/system/islands`

## Objetivo

Permitir que o dono da ilha ligue/desligue comportamentos do mundo **dentro da área da
própria ilha**, sem afetar o mundo inteiro nem outras ilhas.

O framework de settings já existia (`IslandSetting`, `Island.settings`, `SettingsView`) com
uma única entrada (`MobSpawn`, que controla o spawn ao quebrar bloco da fase). Este
documento cobre a expansão para sete settings e o ajuste de GUI/permissão que ela exige.

## Escopo

Settings finais (todas `defaultEnabled = true`, ou seja, comportamento atual preservado):

| Setting | Evento(s) | Observação |
|---|---|---|
| `MobSpawn` (existente) | quebra de bloco da fase | spawn de fase, já implementado em `Island.handleMobSpawn` |
| `NaturalMobSpawn` | `CreatureSpawnEvent` | só reasons `NATURAL`, `PATROL`, `REINFORCEMENTS`. Spawn de fase e minions usam `world.spawnEntity` (`CUSTOM`) e não são afetados |
| `Redstone` | `BlockRedstoneEvent` (`newCurrent = 0`), `BlockPistonExtendEvent`, `BlockPistonRetractEvent` | alavanca/botão continuam alternando, o sinal só não propaga |
| `LeafDecay` | `LeavesDecayEvent` | |
| `LiquidFlow` | `BlockFromToEvent` | checa `event.toBlock.location` |
| `FireSpread` | `BlockIgniteEvent` (causas `SPREAD`, `LAVA`), `BlockBurnEvent` | acender com flint & steel continua liberado |
| `MobGriefing` | `EntityChangeBlockEvent` | só entidades vivas não-jogador. Explosões já são canceladas globalmente em `IslandListener.onEntityExplode`, então creeper já não causava dano a blocos |

### Fora de escopo (registrado)

- `BlockGrowth`: conflita com o upgrade `CropGrowth`, que já cancela `BlockGrowEvent` por
  chance conforme nível (`IslandListener.onBlockGrowth`). Dois controles no mesmo evento
  confunde o jogador.
- Pacote de visitante (`VisitorPvp`, `VisitorInteract`, `VisitorPickup`, `TntDamage`):
  exige revisar `Island.isTrustAllowed` e o cancelamento global de explosão.

## Restrição à área da ilha

Todo handler resolve a ilha por `IslandManager.getIslandAt(location)`, usando sempre a
location do **bloco ou entidade do evento** — nunca `player.location`. `getIslandAt` é
lookup O(1) por grid mais checagem de bounds pelo `size` da ilha. Bloco fora de qualquer
ilha mantém comportamento vanilla.

Fast path: se `island.settings == null` (a ilha nunca alterou nenhuma setting), o handler
retorna imediatamente. Importa em `BlockRedstoneEvent`, que dispara muitas vezes por tick.

## Componentes

### `island/IslandSetting.kt`

Enum com as sete entradas: `displayName`, `icon` (`Material`), `enabledLore`,
`disabledLore`, `defaultEnabled`.

### `island/IslandSettings.kt` (novo)

Objeto puro, sem dependência de Bukkit em runtime, com a resolução de estado:

```kotlin
fun isEnabled(settings: Map<String, Boolean>?, setting: IslandSetting): Boolean
```

`Island.isSettingEnabled` passa a delegar para ele. Existe para permitir teste unitário —
`Island` não é construível fora do servidor porque `currentWorldBorder` chama
`Bukkit.createWorldBorder()` na construção.

### `listener/IslandSettingListener.kt` (novo)

Aplica as settings nos eventos do mundo. Todos os handlers seguem o mesmo formato:
resolve a ilha pela location do evento, checa a setting, cancela. Nenhuma regra de
permissão/trust — isso continua em `IslandListener`.

Registrado em `OneBlockPlugin.setupListeners()`.

### `view/SettingsView.kt`

Passa de 3 para 5 linhas. Settings em grid `row = 2 + index / 7`, `col = 2 + index % 7`
(rows 2..4, cols 2..8 → 21 slots, sete ocupados). Botão de voltar em `(5, 5)`.

Gate de dono: apenas `island.ownerUuid == player.uniqueId` pode alternar. Membro vê o
estado atual, e a última linha da lore vira "Apenas o dono pode alterar."; clique de
não-dono responde com mensagem em vermelho e som `BLOCK_NOTE_BLOCK_BASS`.

### Persistência

Nenhuma mudança de schema. `Island.settings: MutableMap<String, Boolean>?` já é persistido
pelo `IslandRepository` via codec POJO, indexado pelo `name` do enum. Ilhas já salvas
continuam válidas: chave ausente = `defaultEnabled`.

## Testes

O módulo `islands` não tinha testes. Passa a ter Kotest (`kotest-runner`,
`kotest-assertions`, `useJUnitPlatform()`), mais `testImplementation(libs.paperApi)` porque
`IslandSetting` referencia `org.bukkit.Material`.

Coberto: `IslandSettings.isEnabled` (mapa nulo, chave ausente, valor explícito) e os
conjuntos de reasons/causas do listener. Handlers de evento em si exigiriam mock de Bukkit
e ficam fora.

O módulo depende de `lib:connection`, então rodar os testes exige modo local:

```bash
./gradlew :plugins:oneblock:system:islands:test -Penv=local
```

## Arquivos tocados

- `island/IslandSetting.kt` — +6 entradas
- `island/IslandSettings.kt` — novo
- `island/Island.kt` — `isSettingEnabled` delega para `IslandSettings`
- `listener/IslandSettingListener.kt` — novo
- `view/SettingsView.kt` — layout de 5 linhas + gate de dono
- `plugin/.../OneBlockPlugin.kt` — registro do listener
- `system/islands/build.gradle.kts` — Kotest + paper-api de teste
- `src/test/kotlin/.../IslandSettingsTest.kt` — novo
