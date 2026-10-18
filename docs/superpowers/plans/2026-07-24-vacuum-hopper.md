# Vacuum Hopper — Plano de Implementação

> **Para agentes executores:** SUB-SKILL OBRIGATÓRIA: use `superpowers:subagent-driven-development` (recomendado) ou `superpowers:executing-plans` para implementar tarefa a tarefa. Os passos usam checkbox (`- [ ]`).

**Design de origem:** [2026-07-24-vacuum-hopper-design.md](../specs/2026-07-24-vacuum-hopper-design.md)

**Goal:** Adicionar ao módulo `custom-items` um eixo `CustomBlock` e, sobre ele, o Vacuum Hopper — bloco `CONDUIT` colocável que suga drops num raio, filtra por material, guarda num armazenamento com GUI e evolui por upgrades pagos em `coins`.

**Architecture:** O módulo passa a ter dois eixos de framework (`item/`, `block/`) e uma pasta `content/` por feature. O estado do bloco vive em `PlacedBlock` (Mongo, coleção `custom_blocks`), é indexado em memória por `PlacedBlockManager` com flush em lote, e viaja dentro do item via PDC quando o bloco é quebrado. Uma task global de 1 tick dispara `CustomBlock.tick` no intervalo de cada bloco.

**Tech Stack:** Kotlin 2.3 / JDK 21, Paper 1.21.x, MongoDB (`lib:connection`), inventory-framework 3.7.1, Aikar ACF, Adventure MiniMessage, Kotest 5.9 (JUnit 5).

## Global Constraints

- **Nunca rode `git commit` nem `git push`.** Os passos de checkpoint pedem que o usuário comite. Isso vale para todas as tarefas.
- Comentários e strings visíveis ao jogador em **português**.
- `paper-api` é `compileOnly` — **não está no classpath de teste**. Qualquer classe que importe `org.bukkit.*` é intestável em unidade. Toda lógica que precisa de teste mora em arquivo livre de Bukkit.
- Todo comando Gradle usado aqui contém `local` no nome da task, senão o build entra em modo produção e falha resolvendo `com.redescreen:sdk:1.0`.
- Nada em `content/` pode ser importado por `item/` ou `block/`. A dependência aponta só para dentro do framework.
- `custom-items` não pode ser dependência de `islands`, `craft`, `currencies` ou `minions` — isso criaria ciclo.
- Pacote raiz do módulo: `one_block.system.custom_items`.
- Namespace das `NamespacedKey`: `"oneblock"`.
- Moeda dos upgrades: `"coins"`.

## Estrutura de arquivos

```
plugins/oneblock/system/custom-items/src/main/kotlin/one_block/system/custom_items/
├── CustomItemSystem.kt                    MODIFICA — wiring dos dois eixos
├── item/                                  MOVE — framework do eixo item
│   ├── CustomItem.kt  CustomItemKeys.kt  CustomItemTask.kt
│   ├── CustomItemRegistry.kt  CustomItemListener.kt  CustomItemCommand.kt
│   └── context/ InteractContext.kt  EntityContext.kt  ConsumeContext.kt
├── block/                                 NOVO — framework do eixo bloco
│   ├── FilterMode.kt        enum puro
│   ├── FilterRules.kt       predicado puro do filtro
│   ├── StateCodec.kt        encode/decode de string puro
│   ├── BlockUpgrade.kt      interface pura
│   ├── PlacedBlock.kt       modelo persistido
│   ├── PlacedBlockRepository.kt
│   ├── PlacedBlockCodec.kt  leitura/escrita do estado no PDC
│   ├── PlacedBlockManager.kt
│   ├── CustomBlock.kt  CustomBlockKeys.kt  CustomBlockRegistry.kt
│   ├── CustomBlockListener.kt  CustomBlockCommand.kt
│   └── context/ PlaceContext.kt  BreakContext.kt  BlockClickContext.kt
├── cooldown/CooldownStore.kt              PERMANECE
└── content/
    ├── magnet/MagnetItem.kt               MOVE
    └── vacuum_hopper/
        ├── VacuumHopperUpgrade.kt         enum puro (stats + custos)
        ├── VacuumHopper.kt                CustomBlock + sucção + receita
        └── view/ VacuumHopperView.kt  HopperStorageView.kt
                  HopperFilterView.kt  HopperUpgradesView.kt
```

Arquivos livres de Bukkit (testáveis): `FilterMode`, `FilterRules`, `StateCodec`, `BlockUpgrade`, `VacuumHopperUpgrade`.

---

## Task 1: Reorganização de pacotes e dependências do módulo

**Files:**
- Move: os 10 arquivos de `api/`, `registry/`, `listener/`, `command/`, `item/` para o novo desenho
- Modify: `plugins/oneblock/system/custom-items/build.gradle.kts`
- Modify: `plugins/oneblock/plugin/src/main/kotlin/io/github/badnotice/one_block/OneBlockPlugin.kt`
- Test: `plugins/oneblock/system/custom-items/src/test/kotlin/one_block/system/custom_items/CooldownStoreTest.kt` (não muda)

**Interfaces:**
- Consome: nada.
- Produz: pacotes `one_block.system.custom_items.item`, `...item.context`, `...content.magnet`; task Gradle `localTest` no módulo.

- [ ] **Passo 1: Mover os arquivos do eixo item**

Do diretório `plugins/oneblock/system/custom-items/src/main/kotlin/one_block/system/custom_items/`:

| De | Para |
|---|---|
| `api/CustomItem.kt` | `item/CustomItem.kt` |
| `api/CustomItemKeys.kt` | `item/CustomItemKeys.kt` |
| `api/CustomItemTask.kt` | `item/CustomItemTask.kt` |
| `api/InteractContext.kt` | `item/context/InteractContext.kt` |
| `api/EntityContext.kt` | `item/context/EntityContext.kt` |
| `api/ConsumeContext.kt` | `item/context/ConsumeContext.kt` |
| `registry/CustomItemRegistry.kt` | `item/CustomItemRegistry.kt` |
| `listener/CustomItemListener.kt` | `item/CustomItemListener.kt` |
| `command/CustomItemCommand.kt` | `item/CustomItemCommand.kt` |
| `item/MagnetItem.kt` | `content/magnet/MagnetItem.kt` |

Remova os diretórios vazios `api/`, `registry/`, `listener/`, `command/`.

- [ ] **Passo 2: Corrigir os `package` e `import` dos arquivos movidos**

Novos `package`:
- `one_block.system.custom_items.item` — `CustomItem.kt`, `CustomItemKeys.kt`, `CustomItemTask.kt`, `CustomItemRegistry.kt`, `CustomItemListener.kt`, `CustomItemCommand.kt`
- `one_block.system.custom_items.item.context` — `InteractContext.kt`, `EntityContext.kt`, `ConsumeContext.kt`
- `one_block.system.custom_items.content.magnet` — `MagnetItem.kt`

Imports a acertar:
- `CustomItem.kt` passa a importar `one_block.system.custom_items.item.context.InteractContext`, `...EntityContext`, `...ConsumeContext`.
- `CustomItemListener.kt` importa `one_block.system.custom_items.item.context.*` e perde os imports de `...api.*` e `...registry.CustomItemRegistry` (agora mesmo pacote).
- `CustomItemCommand.kt` e `CustomItemRegistry.kt` perdem o import de `...api.CustomItem` / `...api.CustomItemKeys` (mesmo pacote agora).
- `MagnetItem.kt` importa `one_block.system.custom_items.item.CustomItem` e `one_block.system.custom_items.item.context.InteractContext`.
- `CustomItemSystem.kt` passa a importar `one_block.system.custom_items.content.magnet.MagnetItem`, `one_block.system.custom_items.item.CustomItemListener` e `one_block.system.custom_items.item.CustomItemRegistry`.

O teste `CooldownStoreTest` fica onde está — `CooldownStore` não se move.

- [ ] **Passo 3: Corrigir o único import externo ao módulo**

Em `OneBlockPlugin.kt`, trocar:

```kotlin
import one_block.system.custom_items.command.CustomItemCommand
```

por:

```kotlin
import one_block.system.custom_items.item.CustomItemCommand
```

- [ ] **Passo 4: Adicionar as dependências e a task `localTest`**

Substituir todo o conteúdo de `plugins/oneblock/system/custom-items/build.gradle.kts` por:

```kotlin
plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.plugins.oneblock.system.islands)
    compileOnly(projects.plugins.oneblock.system.craft)
    compileOnly(projects.plugins.oneblock.system.currencies)
    compileOnly(projects.plugins.oneblock.system.minions)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}

// O modo do build vem do nome da task conter "local" (ver CLAUDE.md). Um `test`
// puro resolveria `com.redescreen:sdk`, que só existe em produção — daí este alias.
tasks.register("localTest") {
    group = "verification"
    description = "Roda os testes do módulo em modo local."
    dependsOn(tasks.named("test"))
}
```

- [ ] **Passo 5: Rodar os testes existentes**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`, `CooldownStoreTest` passando.

- [ ] **Passo 6: Compilar o plugin inteiro**

```bash
./gradlew :plugins:oneblock:plugin:localJar
```

Esperado: `BUILD SUCCESSFUL`. Se falhar com `unresolved reference`, sobrou import antigo de `custom_items.api`/`.registry`/`.listener`/`.command`.

- [ ] **Passo 7: Checkpoint**

Pare e peça ao usuário que revise e comite. Sugestão de mensagem: `refactor(custom-items): reorganiza módulo em eixos item/block e content`.

---

## Task 2: Núcleo puro do eixo bloco (filtro, codec de string, upgrade)

**Files:**
- Create: `block/FilterMode.kt`, `block/FilterRules.kt`, `block/StateCodec.kt`, `block/BlockUpgrade.kt`
- Test: `src/test/kotlin/one_block/system/custom_items/block/FilterRulesTest.kt`, `.../block/StateCodecTest.kt`

Raiz omitida nos caminhos: `plugins/oneblock/system/custom-items/src/main/kotlin/one_block/system/custom_items/`.

**Interfaces:**
- Consome: nada.
- Produz:
  - `enum class FilterMode(val prettyName: String, val description: String)` com `WHITELIST`, `BLACKLIST` e `fun next(): FilterMode`
  - `object FilterRules { fun accepts(mode: FilterMode, filter: Set<String>, materialName: String): Boolean }`
  - `object StateCodec` com `encodeUpgrades(Map<String,Int>): String`, `decodeUpgrades(String): MutableMap<String,Int>`, `encodeFilter(Set<String>): String`, `decodeFilter(String): MutableSet<String>`, `encodeStorage(List<String>): String`, `decodeStorage(String): MutableList<String>`
  - `interface BlockUpgrade { val id: String; val prettyName: String; val maxLevel: Int; val lore: List<String>; fun costFor(level: Int): Double; fun statLine(level: Int): String }`

- [ ] **Passo 1: Escrever os testes que falham**

`src/test/kotlin/one_block/system/custom_items/block/FilterRulesTest.kt`:

```kotlin
package one_block.system.custom_items.block

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FilterRulesTest : StringSpec({

    "lista vazia aceita tudo em qualquer modo" {
        FilterRules.accepts(FilterMode.WHITELIST, emptySet(), "COBBLESTONE") shouldBe true
        FilterRules.accepts(FilterMode.BLACKLIST, emptySet(), "COBBLESTONE") shouldBe true
    }

    "whitelist aceita só o que está listado" {
        val filter = setOf("DIAMOND", "EMERALD")
        FilterRules.accepts(FilterMode.WHITELIST, filter, "DIAMOND") shouldBe true
        FilterRules.accepts(FilterMode.WHITELIST, filter, "COBBLESTONE") shouldBe false
    }

    "blacklist rejeita o que está listado" {
        val filter = setOf("COBBLESTONE")
        FilterRules.accepts(FilterMode.BLACKLIST, filter, "COBBLESTONE") shouldBe false
        FilterRules.accepts(FilterMode.BLACKLIST, filter, "DIAMOND") shouldBe true
    }

    "next alterna entre os dois modos e volta" {
        FilterMode.WHITELIST.next() shouldBe FilterMode.BLACKLIST
        FilterMode.BLACKLIST.next() shouldBe FilterMode.WHITELIST
    }
})
```

`src/test/kotlin/one_block/system/custom_items/block/StateCodecTest.kt`:

```kotlin
package one_block.system.custom_items.block

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StateCodecTest : StringSpec({

    "upgrades fazem round-trip" {
        val upgrades = mapOf("vacuum_radius" to 2, "vacuum_storage" to 3)
        StateCodec.decodeUpgrades(StateCodec.encodeUpgrades(upgrades)) shouldBe upgrades
    }

    "mapa vazio vira string vazia e volta vazio" {
        StateCodec.encodeUpgrades(emptyMap()) shouldBe ""
        StateCodec.decodeUpgrades("") shouldBe mutableMapOf()
    }

    "entrada malformada de upgrade é ignorada" {
        StateCodec.decodeUpgrades("vacuum_radius:2;lixo;vacuum_speed:x;vacuum_storage:1") shouldBe
            mutableMapOf("vacuum_radius" to 2, "vacuum_storage" to 1)
    }

    "filtro faz round-trip preservando os nomes" {
        val filter = setOf("COBBLESTONE", "DIRT")
        StateCodec.decodeFilter(StateCodec.encodeFilter(filter)) shouldBe filter
    }

    "filtro vazio não gera entrada fantasma" {
        StateCodec.decodeFilter("") shouldBe mutableSetOf<String>()
    }

    "storage usa quebra de linha e ignora entradas em branco" {
        val entries = listOf("YWJj", "ZGVm")
        StateCodec.encodeStorage(entries) shouldBe "YWJj\nZGVm"
        StateCodec.decodeStorage("YWJj\n\nZGVm") shouldBe mutableListOf("YWJj", "ZGVm")
    }
})
```

- [ ] **Passo 2: Rodar e confirmar que falha**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: FAIL na compilação do sourceSet de teste, com `unresolved reference: FilterRules` / `StateCodec` / `FilterMode`.

- [ ] **Passo 3: Implementar `FilterMode`**

`block/FilterMode.kt`:

```kotlin
package one_block.system.custom_items.block

/** Como a lista de filtro de um [PlacedBlock] é interpretada. */
enum class FilterMode(val prettyName: String, val description: String) {
    /** Só guarda os itens que estão na lista. */
    WHITELIST("Aceitar", "Guarda apenas os itens da lista."),

    /** Guarda tudo, menos os itens da lista. */
    BLACKLIST("Filtrar", "Guarda tudo, exceto os itens da lista.");

    fun next(): FilterMode = entries[(ordinal + 1) % entries.size]
}
```

- [ ] **Passo 4: Implementar `FilterRules`**

`block/FilterRules.kt`:

```kotlin
package one_block.system.custom_items.block

/**
 * Predicado do filtro, isolado do Bukkit para poder ser testado em unidade
 * (paper-api é compileOnly e não existe no classpath de teste).
 */
object FilterRules {

    /** `true` se o bloco deve guardar um item desse [materialName]. */
    fun accepts(mode: FilterMode, filter: Set<String>, materialName: String): Boolean {
        if (filter.isEmpty()) return true
        val listed = filter.contains(materialName)
        return when (mode) {
            FilterMode.WHITELIST -> listed
            FilterMode.BLACKLIST -> !listed
        }
    }
}
```

- [ ] **Passo 5: Implementar `StateCodec`**

`block/StateCodec.kt`:

```kotlin
package one_block.system.custom_items.block

/**
 * Codificação do estado do bloco em strings, para caber no PDC do item.
 *
 * Delimitadores: base64 padrão não usa `\n`, e nome de [org.bukkit.Material]
 * não usa `;` — daí a escolha de cada separador.
 */
object StateCodec {

    private const val ENTRY_DELIMITER = ";"
    private const val LEVEL_DELIMITER = ":"
    private const val FILTER_DELIMITER = ";"
    private const val STORAGE_DELIMITER = "\n"

    fun encodeUpgrades(upgrades: Map<String, Int>): String =
        upgrades.entries.joinToString(ENTRY_DELIMITER) { (id, level) -> "$id$LEVEL_DELIMITER$level" }

    fun decodeUpgrades(raw: String): MutableMap<String, Int> {
        val result = mutableMapOf<String, Int>()
        if (raw.isBlank()) return result

        raw.split(ENTRY_DELIMITER).forEach { entry ->
            if (entry.isBlank()) return@forEach
            val parts = entry.split(LEVEL_DELIMITER)
            if (parts.size != 2) return@forEach
            val level = parts[1].toIntOrNull() ?: return@forEach
            result[parts[0]] = level
        }
        return result
    }

    fun encodeFilter(filter: Set<String>): String = filter.joinToString(FILTER_DELIMITER)

    fun decodeFilter(raw: String): MutableSet<String> =
        raw.split(FILTER_DELIMITER).filter { it.isNotBlank() }.toMutableSet()

    fun encodeStorage(entries: List<String>): String = entries.joinToString(STORAGE_DELIMITER)

    fun decodeStorage(raw: String): MutableList<String> =
        raw.split(STORAGE_DELIMITER).filter { it.isNotBlank() }.toMutableList()
}
```

- [ ] **Passo 6: Implementar `BlockUpgrade`**

`block/BlockUpgrade.kt`:

```kotlin
package one_block.system.custom_items.block

/**
 * Um upgrade aplicável a um [CustomBlock]. É interface (e não enum global) para
 * cada bloco declarar o seu próprio conjunto sem enum compartilhado.
 */
interface BlockUpgrade {
    val id: String
    val prettyName: String
    val maxLevel: Int
    val lore: List<String>

    /** Custo para ir de [level] para `level + 1`. Nível fora da faixa devolve `0.0`. */
    fun costFor(level: Int): Double

    /** Linha de status em MiniMessage, já com o valor do [level]. */
    fun statLine(level: Int): String
}
```

- [ ] **Passo 7: Rodar e confirmar que passa**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`, 10 testes passando (5 do `CooldownStore` + 4 do `FilterRules` + 6 do `StateCodec`).

- [ ] **Passo 8: Checkpoint**

Peça o commit ao usuário. Sugestão: `feat(custom-items): núcleo puro do eixo bloco (filtro, codec, upgrade)`.

---

## Task 3: Upgrades do Vacuum Hopper

**Files:**
- Create: `content/vacuum_hopper/VacuumHopperUpgrade.kt`
- Test: `src/test/kotlin/one_block/system/custom_items/content/vacuum_hopper/VacuumHopperUpgradeTest.kt`

**Interfaces:**
- Consome: `BlockUpgrade` (Task 2).
- Produz: `enum class VacuumHopperUpgrade : BlockUpgrade` com entradas `Radius`, `Storage`, `Speed`; companion com `radiusFor(level: Int): Double`, `storageSlotsFor(level: Int): Int`, `tickIntervalFor(level: Int): Long` e as constantes `BASE_RADIUS`, `BASE_STORAGE_SLOTS`, `BASE_TICK_INTERVAL`, `LIMIT_PER_ISLAND`, `UPGRADE_CURRENCY`.

- [ ] **Passo 1: Escrever o teste que falha**

`src/test/kotlin/one_block/system/custom_items/content/vacuum_hopper/VacuumHopperUpgradeTest.kt`:

```kotlin
package one_block.system.custom_items.content.vacuum_hopper

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class VacuumHopperUpgradeTest : StringSpec({

    "raio sobe 2 por nível" {
        VacuumHopperUpgrade.radiusFor(0) shouldBe 4.0
        VacuumHopperUpgrade.radiusFor(1) shouldBe 6.0
        VacuumHopperUpgrade.radiusFor(2) shouldBe 8.0
        VacuumHopperUpgrade.radiusFor(3) shouldBe 10.0
    }

    "armazenamento no nível máximo bate exatamente o teto do inventário Bukkit" {
        VacuumHopperUpgrade.storageSlotsFor(0) shouldBe 27
        VacuumHopperUpgrade.storageSlotsFor(1) shouldBe 36
        VacuumHopperUpgrade.storageSlotsFor(2) shouldBe 45
        VacuumHopperUpgrade.storageSlotsFor(3) shouldBe 54
    }

    "velocidade desce 5 ticks por nível" {
        VacuumHopperUpgrade.tickIntervalFor(0) shouldBe 20L
        VacuumHopperUpgrade.tickIntervalFor(3) shouldBe 5L
    }

    "nível fora da faixa satura no máximo" {
        VacuumHopperUpgrade.radiusFor(99) shouldBe 10.0
        VacuumHopperUpgrade.storageSlotsFor(99) shouldBe 54
        VacuumHopperUpgrade.tickIntervalFor(99) shouldBe 5L
        VacuumHopperUpgrade.radiusFor(-1) shouldBe 4.0
    }

    "custo de cada upgrade segue a tabela do design" {
        VacuumHopperUpgrade.Radius.costFor(0) shouldBe 5_000.0
        VacuumHopperUpgrade.Radius.costFor(2) shouldBe 40_000.0
        VacuumHopperUpgrade.Storage.costFor(0) shouldBe 7_500.0
        VacuumHopperUpgrade.Speed.costFor(2) shouldBe 60_000.0
    }

    "custo de nível já no máximo é zero" {
        VacuumHopperUpgrade.Radius.costFor(3) shouldBe 0.0
        VacuumHopperUpgrade.Speed.costFor(99) shouldBe 0.0
    }

    "statLine mostra o valor resultante do nível" {
        VacuumHopperUpgrade.Radius.statLine(1) shouldBe "<yellow>6 blocos"
        VacuumHopperUpgrade.Storage.statLine(2) shouldBe "<yellow>45 slots"
        VacuumHopperUpgrade.Speed.statLine(3) shouldBe "<yellow>5 ticks"
    }
})
```

- [ ] **Passo 2: Rodar e confirmar que falha**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: FAIL com `unresolved reference: VacuumHopperUpgrade`.

- [ ] **Passo 3: Implementar o enum**

`content/vacuum_hopper/VacuumHopperUpgrade.kt`:

```kotlin
package one_block.system.custom_items.content.vacuum_hopper

import one_block.system.custom_items.block.BlockUpgrade

/**
 * Upgrades do Vacuum Hopper. Arquivo sem Bukkit de propósito: é o que permite
 * testar os stats e os custos em unidade.
 */
enum class VacuumHopperUpgrade(
    override val id: String,
    override val prettyName: String,
    override val maxLevel: Int,
    override val lore: List<String>,
    private val costs: List<Double>,
) : BlockUpgrade {

    Radius(
        id = "vacuum_radius",
        prettyName = "Raio",
        maxLevel = 3,
        lore = listOf("Aumenta o alcance", "da sucção."),
        costs = listOf(5_000.0, 15_000.0, 40_000.0),
    ) {
        override fun statLine(level: Int) = "<yellow>${radiusFor(level).toInt()} blocos"
    },

    Storage(
        id = "vacuum_storage",
        prettyName = "Armazenamento",
        maxLevel = 3,
        lore = listOf("Aumenta o espaço de", "armazenamento do bloco."),
        costs = listOf(7_500.0, 20_000.0, 50_000.0),
    ) {
        override fun statLine(level: Int) = "<yellow>${storageSlotsFor(level)} slots"
    },

    Speed(
        id = "vacuum_speed",
        prettyName = "Velocidade",
        maxLevel = 3,
        lore = listOf("Diminui o tempo entre", "cada sucção."),
        costs = listOf(10_000.0, 25_000.0, 60_000.0),
    ) {
        override fun statLine(level: Int) = "<yellow>${tickIntervalFor(level)} ticks"
    };

    override fun costFor(level: Int): Double = costs.getOrElse(level) { 0.0 }

    abstract override fun statLine(level: Int): String

    companion object {
        const val BASE_RADIUS = 4.0
        const val RADIUS_PER_LEVEL = 2.0

        const val BASE_STORAGE_SLOTS = 27
        const val STORAGE_SLOTS_PER_LEVEL = 9

        const val BASE_TICK_INTERVAL = 20L
        const val TICK_INTERVAL_PER_LEVEL = 5L

        const val MAX_LEVEL = 3

        const val LIMIT_PER_ISLAND = 5
        const val UPGRADE_CURRENCY = "coins"

        fun radiusFor(level: Int): Double =
            BASE_RADIUS + level.coerceIn(0, MAX_LEVEL) * RADIUS_PER_LEVEL

        // 27 + 3*9 = 54, o máximo de um inventário Bukkit. Subir MAX_LEVEL sem
        // paginar a GUI estoura o createInventory em runtime.
        fun storageSlotsFor(level: Int): Int =
            BASE_STORAGE_SLOTS + level.coerceIn(0, MAX_LEVEL) * STORAGE_SLOTS_PER_LEVEL

        fun tickIntervalFor(level: Int): Long =
            BASE_TICK_INTERVAL - level.coerceIn(0, MAX_LEVEL) * TICK_INTERVAL_PER_LEVEL
    }
}
```

- [ ] **Passo 4: Rodar e confirmar que passa**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`, 7 testes novos passando.

- [ ] **Passo 5: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): upgrades do vacuum hopper`.

---


## Task 4: Modelo persistido — `PlacedBlock`, repositório e chaves do PDC

**Files:**
- Create: `block/PlacedBlock.kt`, `block/PlacedBlockRepository.kt`, `block/CustomBlockKeys.kt`

**Interfaces:**
- Consome: `FilterMode`, `FilterRules` (Task 2); `SimpleLocation` (`one_block.libs.coordinate`); `ItemSerializer` (`one_block.libs.item_builder`).
- Produz:
  - `data class PlacedBlock(uuid: UUID, location: SimpleLocation, blockId: String, ownerUuid: UUID, enabled: Boolean, upgrades: MutableMap<String,Int>, storage: MutableList<String>, filterMode: FilterMode, filter: MutableSet<String>, notifiedFull: Boolean)` com `getUpgradeLevel(id|upgrade)`, `setUpgradeLevel(upgrade, level)`, `accepts(Material)`, `storageItems()`, `setStorage(List<ItemStack>)`, `isStorageFull(capacity: Int)`, `addToStorage(items: List<ItemStack>, capacity: Int): List<ItemStack>`
  - `class PlacedBlockRepository` com `toList()`, `update(PlacedBlock)`, `updateAll(List<PlacedBlock>)`, `remove(UUID)`
  - `object CustomBlockKeys` com `ID`, `UUID`, `UPGRADES`, `STORAGE`, `FILTER_MODE`, `FILTER`, `ENABLED`

Sem teste unitário aqui: todo o arquivo toca `org.bukkit`, que não existe no classpath de teste. A verificação é compilar; o comportamento entra na verificação manual da Task 11.

- [ ] **Passo 1: Criar `CustomBlockKeys`**

`block/CustomBlockKeys.kt`:

```kotlin
package one_block.system.custom_items.block

import org.bukkit.NamespacedKey

/** Chaves do PDC usadas para carregar o estado do bloco dentro do item. */
object CustomBlockKeys {
    /** Identidade do [CustomBlock]. Sempre presente, mesmo em item recém-craftado. */
    val ID = NamespacedKey("oneblock", "custom_block_id")

    val UUID = NamespacedKey("oneblock", "custom_block_uuid")
    val UPGRADES = NamespacedKey("oneblock", "custom_block_upgrades")
    val STORAGE = NamespacedKey("oneblock", "custom_block_storage")
    val FILTER_MODE = NamespacedKey("oneblock", "custom_block_filter_mode")
    val FILTER = NamespacedKey("oneblock", "custom_block_filter")
    val ENABLED = NamespacedKey("oneblock", "custom_block_enabled")
}
```

- [ ] **Passo 2: Criar `PlacedBlock`**

`block/PlacedBlock.kt`:

```kotlin
package one_block.system.custom_items.block

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.item_builder.ItemSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * Estado de um [CustomBlock] colocado no mundo. Persistido na coleção
 * `custom_blocks` e embutido no item quando o bloco é quebrado.
 *
 * A capacidade não é campo: ela deriva dos upgrades, e quem sabe calculá-la é o
 * [CustomBlock] correspondente — por isso os métodos de armazenamento a recebem.
 */
data class PlacedBlock(
    val uuid: UUID,
    val location: SimpleLocation,
    /** Id do [CustomBlock] — resolvido no [CustomBlockRegistry]. */
    val blockId: String,
    val ownerUuid: UUID,
    var enabled: Boolean = true,
    val upgrades: MutableMap<String, Int> = mutableMapOf(),
    /** Itens guardados, cada entrada é um ItemStack serializado em base64. */
    val storage: MutableList<String> = mutableListOf(),
    var filterMode: FilterMode = FilterMode.BLACKLIST,
    /** Materiais na lista de filtro, pelo nome do [Material]. */
    val filter: MutableSet<String> = mutableSetOf(),
    /** Evita repetir o aviso de armazenamento cheio a cada tick. */
    var notifiedFull: Boolean = false,
) {

    fun getUpgradeLevel(id: String, def: Int = 0): Int = upgrades.getOrDefault(id, def)

    fun getUpgradeLevel(upgrade: BlockUpgrade, def: Int = 0): Int = upgrades.getOrDefault(upgrade.id, def)

    fun setUpgradeLevel(upgrade: BlockUpgrade, level: Int) {
        upgrades[upgrade.id] = level
    }

    /** `true` se o bloco deve guardar um item desse [material]. */
    fun accepts(material: Material): Boolean = FilterRules.accepts(filterMode, filter, material.name)

    /** Itens guardados, desserializados. Entradas inválidas são ignoradas. */
    fun storageItems(): List<ItemStack> =
        storage.mapNotNull { runCatching { ItemSerializer.deserialize(it) }.getOrNull() }

    /** Substitui todo o conteúdo do armazenamento. */
    fun setStorage(items: List<ItemStack>) {
        storage.clear()
        items.forEach { storage.add(ItemSerializer.serialize(it)) }
    }

    /** `true` quando não há slot vazio nem stack parcial. */
    fun isStorageFull(capacity: Int): Boolean {
        val inventory = buildStorageInventory(capacity)
        return inventory.firstEmpty() == -1 &&
            inventory.contents.filterNotNull().all { it.amount >= it.maxStackSize }
    }

    /** Adiciona [items] ao armazenamento e devolve o que não coube. */
    fun addToStorage(items: List<ItemStack>, capacity: Int): List<ItemStack> {
        val inventory = buildStorageInventory(capacity)
        val leftover = inventory.addItem(*items.map { it.clone() }.toTypedArray())
        setStorage(inventory.contents.filterNotNull())
        return leftover.values.toList()
    }

    /** Inventário virtual, só para reaproveitar a lógica de stacking do Bukkit. */
    private fun buildStorageInventory(capacity: Int): Inventory {
        val inventory = Bukkit.createInventory(null, capacity)
        inventory.addItem(*storageItems().toTypedArray())
        return inventory
    }
}
```

- [ ] **Passo 3: Criar `PlacedBlockRepository`**

`block/PlacedBlockRepository.kt`:

```kotlin
package one_block.system.custom_items.block

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.UUID

class PlacedBlockRepository {

    private val collection: MongoCollection<PlacedBlock> = Mongo.getCollection("custom_blocks")
    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlacedBlock> = collection.find().toList()

    fun update(placed: PlacedBlock) {
        collection.replaceOne(Filters.eq("uuid", placed.uuid), placed, replaceOptions)
    }

    fun updateAll(list: List<PlacedBlock>) {
        if (list.isEmpty()) return

        val models = list.map { ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions) }
        collection.bulkWrite(models)
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }
}
```

- [ ] **Passo 4: Compilar**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL` — módulo compila e os testes já existentes continuam passando.

- [ ] **Passo 5: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): modelo e repositório de blocos colocados`.

---

## Task 5: API `CustomBlock`, contextos e registry

**Files:**
- Create: `block/context/PlaceContext.kt`, `block/context/BreakContext.kt`, `block/context/BlockClickContext.kt`, `block/PlacedBlockCodec.kt`, `block/CustomBlock.kt`, `block/CustomBlockRegistry.kt`

**Interfaces:**
- Consome: `PlacedBlock`, `CustomBlockKeys`, `StateCodec`, `FilterMode`, `BlockUpgrade`.
- Produz:
  - `object PlacedBlockCodec { fun write(meta: ItemMeta, placed: PlacedBlock); fun read(item: ItemStack, blockId: String, location: SimpleLocation, ownerUuid: UUID): PlacedBlock? }`
  - `abstract class CustomBlock` com `id`, `material`, `displayName`, `lore`, `itemModel`, `limitPerIsland`, `upgrades`, `storageCapacity(placed): Int`, `tickInterval(placed): Long`, `buildItem(placed: PlacedBlock? = null, amount: Int = 1): ItemStack` e os hooks `onPlace`, `onBreak`, `onRightClick`, `tick`
  - `object CustomBlockRegistry { fun register(block: CustomBlock); fun get(id: String): CustomBlock?; fun all(): Collection<CustomBlock>; fun ids(): Set<String>; fun fromItemStack(item: ItemStack?): CustomBlock? }`
  - `class PlaceContext(player, block, placed, event)`, `class BreakContext(player, block, placed, event)`, `class BlockClickContext(player, block, placed, viewFrame, event)` — todos com `fun cancel()`

- [ ] **Passo 1: Criar os três contextos**

`block/context/PlaceContext.kt`:

```kotlin
package one_block.system.custom_items.block.context

import one_block.system.custom_items.block.PlacedBlock
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable

class PlaceContext(
    val player: Player,
    val block: Block,
    val placed: PlacedBlock,
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }
}
```

`block/context/BreakContext.kt`:

```kotlin
package one_block.system.custom_items.block.context

import one_block.system.custom_items.block.PlacedBlock
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable

class BreakContext(
    val player: Player,
    val block: Block,
    val placed: PlacedBlock,
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }
}
```

`block/context/BlockClickContext.kt`:

```kotlin
package one_block.system.custom_items.block.context

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.custom_items.block.PlacedBlock
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable

/**
 * O [viewFrame] vem no contexto — e não de um global — para o conteúdo em
 * `content/` abrir as suas próprias views sem o framework conhecer nenhuma delas.
 */
class BlockClickContext(
    val player: Player,
    val block: Block,
    val placed: PlacedBlock,
    val viewFrame: ViewFrame,
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }
}
```

- [ ] **Passo 2: Criar `PlacedBlockCodec`**

`block/PlacedBlockCodec.kt`:

```kotlin
package one_block.system.custom_items.block

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.item_builder.nbt
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * Leitura e escrita do estado de um [PlacedBlock] no PDC do item. Isolado para
 * manter [CustomBlock.buildItem] fino e deixar o formato num lugar óbvio para
 * quem for debugar estado perdido.
 */
object PlacedBlockCodec {

    /** Escreve o estado inteiro de [placed] no [meta] do item. */
    fun write(meta: ItemMeta, placed: PlacedBlock) {
        meta.nbt {
            string(CustomBlockKeys.UUID, placed.uuid.toString())
            int(CustomBlockKeys.ENABLED, if (placed.enabled) 1 else 0)
            string(CustomBlockKeys.FILTER_MODE, placed.filterMode.name)

            val upgrades = StateCodec.encodeUpgrades(placed.upgrades)
            if (upgrades.isNotEmpty()) string(CustomBlockKeys.UPGRADES, upgrades)

            if (placed.filter.isNotEmpty()) {
                string(CustomBlockKeys.FILTER, StateCodec.encodeFilter(placed.filter))
            }
            if (placed.storage.isNotEmpty()) {
                string(CustomBlockKeys.STORAGE, StateCodec.encodeStorage(placed.storage))
            }
        }
    }

    /**
     * Reconstrói o [PlacedBlock] guardado no [item]. Devolve `null` quando o item
     * não carrega estado (item novo, vindo de craft ou de comando).
     */
    fun read(item: ItemStack, blockId: String, location: SimpleLocation, ownerUuid: UUID): PlacedBlock? {
        val container = item.itemMeta?.persistentDataContainer ?: return null
        val rawUuid = container.get(CustomBlockKeys.UUID, PersistentDataType.STRING) ?: return null
        val uuid = runCatching { UUID.fromString(rawUuid) }.getOrNull() ?: return null

        val upgrades = container.get(CustomBlockKeys.UPGRADES, PersistentDataType.STRING)
            ?.let { StateCodec.decodeUpgrades(it) }
            ?: mutableMapOf()

        val storage = container.get(CustomBlockKeys.STORAGE, PersistentDataType.STRING)
            ?.let { StateCodec.decodeStorage(it) }
            ?: mutableListOf()

        val filter = container.get(CustomBlockKeys.FILTER, PersistentDataType.STRING)
            ?.let { StateCodec.decodeFilter(it) }
            ?: mutableSetOf()

        val filterMode = container.get(CustomBlockKeys.FILTER_MODE, PersistentDataType.STRING)
            ?.let { runCatching { FilterMode.valueOf(it) }.getOrNull() }
            ?: FilterMode.BLACKLIST

        val enabled = (container.get(CustomBlockKeys.ENABLED, PersistentDataType.INTEGER) ?: 1) != 0

        return PlacedBlock(
            uuid = uuid,
            location = location,
            blockId = blockId,
            ownerUuid = ownerUuid,
            enabled = enabled,
            upgrades = upgrades,
            storage = storage,
            filterMode = filterMode,
            filter = filter,
        )
    }
}
```

- [ ] **Passo 3: Criar `CustomBlock`**

`block/CustomBlock.kt`:

```kotlin
package one_block.system.custom_items.block

import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.block.context.BlockClickContext
import one_block.system.custom_items.block.context.BreakContext
import one_block.system.custom_items.block.context.PlaceContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * Eixo bloco do módulo: conteúdo custom que existe colocado no mundo, com estado
 * próprio em [PlacedBlock]. Paralelo ao eixo item
 * ([one_block.system.custom_items.item.CustomItem]).
 */
abstract class CustomBlock {

    abstract val id: String
    abstract val material: Material
    abstract val displayName: String
    open val lore: List<String> = emptyList()
    open val itemModel: NamespacedKey? = null

    /** Quantos deste bloco cabem numa mesma ilha. */
    open val limitPerIsland: Int = 5

    open val upgrades: List<BlockUpgrade> = emptyList()

    /** Slots do armazenamento com os upgrades de [placed] aplicados. */
    open fun storageCapacity(placed: PlacedBlock): Int = 0

    /** De quantos em quantos ticks o [tick] deste bloco roda. */
    open fun tickInterval(placed: PlacedBlock): Long = 20L

    /**
     * Item que representa o bloco. Com [placed], embute o estado inteiro no PDC —
     * é o que faz quebrar e recolocar preservar armazenamento, filtro e upgrades.
     */
    open fun buildItem(placed: PlacedBlock? = null, amount: Int = 1): ItemStack = itemStack(material) {
        this.amount = amount
        meta<ItemMeta> {
            this@CustomBlock.itemModel?.let { model -> itemModel = model }
            name(this@CustomBlock.displayName)

            setLore {
                this@CustomBlock.lore.forEach { +it }
                if (placed != null) {
                    val evolved = this@CustomBlock.upgrades.filter { placed.getUpgradeLevel(it) > 0 }
                    if (evolved.isNotEmpty()) {
                        +""
                        evolved.forEach { upgrade ->
                            +"<dark_gray>▸ <white>${upgrade.prettyName}: ${upgrade.statLine(placed.getUpgradeLevel(upgrade))}"
                        }
                    }
                    if (placed.storage.isNotEmpty()) {
                        +"<dark_gray>▸ <white>Itens guardados: <aqua>${placed.storage.size}"
                    }
                }
            }

            nbt { string(CustomBlockKeys.ID, this@CustomBlock.id) }
            placed?.let { PlacedBlockCodec.write(this, it) }
        }
    }

    open fun onPlace(ctx: PlaceContext) {}

    open fun onBreak(ctx: BreakContext) {}

    open fun onRightClick(ctx: BlockClickContext) {}

    /** Chamado pela task global do [PlacedBlockManager] no intervalo deste bloco. */
    open fun tick(placed: PlacedBlock) {}
}
```

- [ ] **Passo 4: Criar `CustomBlockRegistry`**

`block/CustomBlockRegistry.kt`:

```kotlin
package one_block.system.custom_items.block

import one_block.libs.instance.Instance
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CustomBlockRegistry {

    private val blocks = mutableMapOf<String, CustomBlock>()

    fun register(block: CustomBlock) {
        if (blocks.containsKey(block.id)) {
            Instance.logger.warning("[custom-items] id de bloco duplicado ignorado: ${block.id}")
            return
        }
        blocks[block.id] = block
    }

    fun get(id: String): CustomBlock? = blocks[id]

    fun all(): Collection<CustomBlock> = blocks.values

    fun ids(): Set<String> = blocks.keys.toSet()

    fun fromItemStack(item: ItemStack?): CustomBlock? {
        if (item == null || item.type.isAir) return null
        val meta = item.itemMeta ?: return null
        val id = meta.persistentDataContainer.get(CustomBlockKeys.ID, PersistentDataType.STRING) ?: return null
        return blocks[id]
    }
}
```

- [ ] **Passo 5: Compilar**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Passo 6: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): api CustomBlock, contextos e registry`.

---

## Task 6: `PlacedBlockManager` — índice em memória, tick global e flush em lote

**Files:**
- Create: `block/PlacedBlockManager.kt`

**Interfaces:**
- Consome: `PlacedBlock`, `PlacedBlockRepository`, `CustomBlockRegistry`, `CustomBlock`; `Instance` e `ScheduledExecutor` (`one_block.libs.instance`); `SimpleLocation`; `IslandManager` (`one_block.system.islands.island`).
- Produz:
  - `object PlacedBlockManager` com `onInit()`, `onDisable()`, `keyOf(block: Block): SimpleLocation`, `get(location: SimpleLocation): PlacedBlock?`, `get(block: Block): PlacedBlock?`, `get(uuid: UUID): PlacedBlock?`, `all(): List<PlacedBlock>`, `countOnIsland(blockId: String, islandUuid: UUID): Int`, `register(placed: PlacedBlock)`, `unregister(location: SimpleLocation): PlacedBlock?`, `markDirty(placed: PlacedBlock)`, `saveNow(placed: PlacedBlock)`

- [ ] **Passo 1: Criar o manager**

`block/PlacedBlockManager.kt`:

```kotlin
package one_block.system.custom_items.block

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.libs.instance.ScheduledExecutor
import one_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Fonte de verdade em runtime dos blocos colocados. Carrega tudo do Mongo no
 * boot, serve as consultas pelos mapas em memória e devolve ao banco em lote.
 *
 * As escritas por tick vão para [dirtyIds] e só descem no flush periódico; as
 * operações que o jogador veria sumir num crash (colocar, quebrar, comprar
 * upgrade, retirar item) usam [saveNow].
 */
object PlacedBlockManager {

    /** Intervalo do flush em lote das entradas sujas. */
    const val SAVE_INTERVAL_SECONDS = 10L

    /** Atraso do boot, para o mundo já estar carregado quando os mapas enchem. */
    private const val INIT_DELAY_TICKS = 40L

    private val byLocation = mutableMapOf<SimpleLocation, PlacedBlock>()
    private val byUuid = mutableMapOf<UUID, PlacedBlock>()
    private val dirtyIds = mutableSetOf<UUID>()

    /** Uuids cujo bloco físico já foi conferido pelo menos uma vez. */
    private val validated = mutableSetOf<UUID>()

    private val repository by lazy { PlacedBlockRepository() }

    private var tickCounter = 0L

    fun onInit() {
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            repository.toList().forEach { index(it) }
            Instance.logger.info("[custom-items] ${byUuid.size} bloco(s) custom carregado(s).")

            Bukkit.getScheduler().runTaskTimer(Instance, Runnable { processTick() }, 1L, 1L)
            ScheduledExecutor.scheduleAtFixedRate(
                { flushDirty() },
                SAVE_INTERVAL_SECONDS,
                SAVE_INTERVAL_SECONDS,
                TimeUnit.SECONDS,
            )
        }, INIT_DELAY_TICKS)
    }

    /** Precisa ser chamado do `OneBlockPlugin.onDisable` — nada faz isso sozinho. */
    fun onDisable() = flushDirty()

    /**
     * Chave canônica de um bloco. `block.location` é o canto do bloco, com yaw e
     * pitch zerados, então dois blocos iguais geram a mesma [SimpleLocation].
     */
    fun keyOf(block: Block): SimpleLocation = SimpleLocation.fromBukkit(block.location)

    fun get(location: SimpleLocation): PlacedBlock? = byLocation[location]

    fun get(block: Block): PlacedBlock? = byLocation[keyOf(block)]

    fun get(uuid: UUID): PlacedBlock? = byUuid[uuid]

    fun all(): List<PlacedBlock> = byUuid.values.toList()

    /**
     * Quantos blocos de [blockId] já existem na ilha [islandUuid]. Varredura
     * linear sobre o mapa: a ordem de grandeza é dezenas de blocos, não paga
     * manter um índice por ilha.
     */
    fun countOnIsland(blockId: String, islandUuid: UUID): Int =
        byUuid.values.count { placed ->
            placed.blockId == blockId && islandUuidOf(placed) == islandUuid
        }

    fun register(placed: PlacedBlock) {
        index(placed)
        saveNow(placed)
    }

    fun unregister(location: SimpleLocation): PlacedBlock? {
        val removed = byLocation.remove(location) ?: return null
        byUuid.remove(removed.uuid)
        validated.remove(removed.uuid)
        synchronized(dirtyIds) { dirtyIds.remove(removed.uuid) }
        repository.remove(removed.uuid)
        return removed
    }

    /** Marca para o flush em lote. Use no caminho quente (tick de sucção). */
    fun markDirty(placed: PlacedBlock) {
        synchronized(dirtyIds) { dirtyIds.add(placed.uuid) }
    }

    /** Persiste imediatamente. Use fora do caminho quente. */
    fun saveNow(placed: PlacedBlock) {
        synchronized(dirtyIds) { dirtyIds.remove(placed.uuid) }
        repository.update(placed)
    }

    private fun index(placed: PlacedBlock) {
        byLocation[placed.location] = placed
        byUuid[placed.uuid] = placed
    }

    private fun islandUuidOf(placed: PlacedBlock): UUID? {
        val bukkit = placed.location.toBukkit() ?: return null
        return IslandManager.getIslandAt(bukkit)?.uuid
    }

    private fun flushDirty() {
        val snapshot = synchronized(dirtyIds) {
            if (dirtyIds.isEmpty()) return
            val ids = dirtyIds.toList()
            dirtyIds.clear()
            ids
        }

        val blocks = snapshot.mapNotNull { byUuid[it] }
        runCatching { repository.updateAll(blocks) }
            .onFailure { Instance.logger.warning("[custom-items] flush de blocos falhou: ${it.message}") }
    }

    private fun processTick() {
        tickCounter++

        byUuid.values.toList().forEach { placed ->
            val block = CustomBlockRegistry.get(placed.blockId) ?: return@forEach
            if (!placed.enabled) return@forEach
            if (!isChunkLoaded(placed)) return@forEach
            if (!isStillValid(placed, block)) return@forEach

            val interval = block.tickInterval(placed).coerceAtLeast(1L)
            if (tickCounter % interval != 0L) return@forEach

            runCatching { block.tick(placed) }
                .onFailure { Instance.logger.warning("[custom-items] tick de '${placed.blockId}' falhou: ${it.message}") }
        }
    }

    private fun isChunkLoaded(placed: PlacedBlock): Boolean {
        val bukkit: Location = placed.location.toBukkit() ?: return false
        val world = bukkit.world ?: return false
        return world.isChunkLoaded(bukkit.blockX shr 4, bukkit.blockZ shr 4)
    }

    /**
     * Validação preguiçosa: no primeiro tick com o chunk carregado, confere se o
     * bloco físico ainda é o esperado. WorldEdit, rollback ou restauração de
     * backup deixariam um registro fantasma ocupando slot do limite por ilha.
     * Validar tudo no boot forçaria carregar todos os chunks — caro e inútil.
     */
    private fun isStillValid(placed: PlacedBlock, custom: CustomBlock): Boolean {
        if (placed.uuid in validated) return true

        val bukkit = placed.location.toBukkit() ?: return false
        if (bukkit.block.type != custom.material) {
            Instance.logger.warning(
                "[custom-items] bloco '${placed.blockId}' sumiu em ${placed.location}; registro removido."
            )
            unregister(placed.location)
            return false
        }

        validated.add(placed.uuid)
        return true
    }
}
```

- [ ] **Passo 2: Compilar**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Passo 3: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): manager de blocos colocados com tick global e flush em lote`.

---

## Task 7: `VacuumHopper` — bloco, sucção e receita

**Files:**
- Create: `content/vacuum_hopper/VacuumHopper.kt`

**Interfaces:**
- Consome: `CustomBlock`, `PlacedBlock`, `PlacedBlockManager`, `BlockClickContext` (Tasks 4–6); `VacuumHopperUpgrade` (Task 3); `ForgeRecipe`/`ForgeRequirement` (`one_block.system.craft.forge`); `Minions.machinePart()` (`one_block.system.minions`).
- Produz:
  - `class VacuumHopper : CustomBlock()` com `id = "vacuum_hopper"`, `radius(placed: PlacedBlock): Double`, `storageCapacity`, `tickInterval`, `tick`, `onRightClick`, `clearFullNotice(placed: PlacedBlock)`
  - `companion object { const val ID = "vacuum_hopper"; fun forgeRecipe(hopper: VacuumHopper): ForgeRecipe }`
- As views só existem na Task 9, então aqui `onRightClick` fica herdado (vazio) e a Task 9 o preenche — assim cada tarefa compila sozinha.

- [ ] **Passo 1: Criar a classe**

`content/vacuum_hopper/VacuumHopper.kt`:

```kotlin
package one_block.system.custom_items.content.vacuum_hopper

import one_block.libs.item_builder.itemStack
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRequirement
import one_block.system.custom_items.block.CustomBlock
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.minions.Minions
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

/**
 * Bloco que suga drops num raio ao redor, filtra por material e guarda num
 * armazenamento próprio com GUI. Primeiro [CustomBlock] do módulo.
 */
class VacuumHopper : CustomBlock() {

    override val id = ID
    override val material = Material.CONDUIT
    override val displayName = "<aqua>Vacuum Hopper"
    override val lore = listOf(
        "<gray>Suga os itens dropados",
        "<gray>num raio ao redor.",
        "",
        "<dark_gray>Clique-direito para abrir."
    )
    override val limitPerIsland = VacuumHopperUpgrade.LIMIT_PER_ISLAND
    override val upgrades = VacuumHopperUpgrade.entries.toList()

    override fun storageCapacity(placed: PlacedBlock): Int =
        VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))

    override fun tickInterval(placed: PlacedBlock): Long =
        VacuumHopperUpgrade.tickIntervalFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Speed))

    /** Alcance da sucção com o upgrade de Raio aplicado. */
    fun radius(placed: PlacedBlock): Double =
        VacuumHopperUpgrade.radiusFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Radius))

    override fun tick(placed: PlacedBlock) {
        val capacity = storageCapacity(placed)
        if (placed.isStorageFull(capacity)) {
            notifyFull(placed)
            return
        }

        val center = placed.location.toBukkit()?.add(0.5, 0.5, 0.5) ?: return
        val world = center.world ?: return
        val range = radius(placed)

        var changed = false

        world.getNearbyEntities(center, range, range, range) { it is Item }.forEach { entity ->
            val drop = entity as Item
            // pickupDelay no máximo marca drop que nunca deve ser coletado.
            if (drop.pickupDelay >= Short.MAX_VALUE.toInt()) return@forEach

            val stack = drop.itemStack
            if (!placed.accepts(stack.type)) return@forEach

            val leftover = placed.addToStorage(listOf(stack), capacity)
            changed = true

            if (leftover.isEmpty()) {
                drop.remove()
            } else {
                // Coube só parte: o drop continua no chão com o que sobrou.
                drop.itemStack = leftover.first()
                notifyFull(placed)
            }
        }

        if (changed) PlacedBlockManager.markDirty(placed)
    }

    /** Chamado quando a GUI abre espaço, para o próximo enchimento avisar de novo. */
    fun clearFullNotice(placed: PlacedBlock) {
        if (!placed.notifiedFull) return
        placed.notifiedFull = false
    }

    private fun notifyFull(placed: PlacedBlock) {
        if (placed.notifiedFull) return
        placed.notifiedFull = true
        PlacedBlockManager.markDirty(placed)

        val owner = Bukkit.getPlayer(placed.ownerUuid) ?: return
        owner.sendRichMessage("<yellow>Seu <aqua>Vacuum Hopper<yellow> está cheio e parou de sugar.")
        owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f)
    }

    companion object {
        const val ID = "vacuum_hopper"

        /** Receita da Forge. Registrada pelo `CustomItemSystem`. */
        fun forgeRecipe(hopper: VacuumHopper) = ForgeRecipe(
            id = "vacuum_hopper",
            prettyName = "Vacuum Hopper",
            icon = hopper.buildItem(),
            result = hopper.buildItem(),
            requirements = listOf(
                ForgeRequirement("Conduit", ItemStack(Material.CONDUIT), 1),
                ForgeRequirement("Barra de Netherite", itemStack(Material.NETHERITE_INGOT) {}, 4),
                ForgeRequirement("Peça Mecânica", Minions.machinePart(), 8),
            ),
        )
    }
}
```

- [ ] **Passo 2: Compilar**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`. Se aparecer `unresolved reference: Minions`, falta `compileOnly(projects.plugins.oneblock.system.minions)` no `build.gradle.kts` (Task 1, passo 4).

- [ ] **Passo 3: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): vacuum hopper com sucção e receita`.

---

## Task 8: `CustomBlockListener` — ciclo de vida e proteção

**Files:**
- Create: `block/CustomBlockListener.kt`

**Interfaces:**
- Consome: `PlacedBlockManager`, `CustomBlockRegistry`, `PlacedBlockCodec`, `PlacedBlock`, os três contextos; `Island`/`IslandManager` (`one_block.system.islands.island`).
- Produz: `class CustomBlockListener(private val viewFrame: ViewFrame) : Listener`.

Esta tarefa é a que evita o registro fantasma (bloco físico some, entrada continua no Mongo ocupando slot do limite por ilha). Explosão e pistão são tratados aqui, não "depois".

- [ ] **Passo 1: Criar o listener**

`block/CustomBlockListener.kt`:

```kotlin
package one_block.system.custom_items.block

import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.system.custom_items.block.context.BlockClickContext
import one_block.system.custom_items.block.context.BreakContext
import one_block.system.custom_items.block.context.PlaceContext
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID

class CustomBlockListener(private val viewFrame: ViewFrame) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val custom = CustomBlockRegistry.fromItemStack(event.itemInHand) ?: return
        val player = event.player
        val block = event.blockPlaced

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode colocar isso aqui.")
            return
        }

        val current = PlacedBlockManager.countOnIsland(custom.id, island.uuid)
        if (current >= custom.limitPerIsland) {
            event.isCancelled = true
            player.sendRichMessage("<red>Limite de <white>${custom.limitPerIsland}<red> por ilha atingido.")
            return
        }

        val location = PlacedBlockManager.keyOf(block)
        // Sem estado no PDC o item é novo (craft ou comando): começa zerado.
        val placed = PlacedBlockCodec.read(event.itemInHand, custom.id, location, player.uniqueId)
            ?: PlacedBlock(
                uuid = UUID.randomUUID(),
                location = location,
                blockId = custom.id,
                ownerUuid = player.uniqueId,
            )

        runHook(custom.id) { custom.onPlace(PlaceContext(player, block, placed, event)) }
        if (event.isCancelled) return

        PlacedBlockManager.register(placed)
        player.sendRichMessage("<green>Bloco colocado. <gray>(${current + 1}/${custom.limitPerIsland})")
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        val placed = PlacedBlockManager.get(block) ?: return
        val custom = CustomBlockRegistry.get(placed.blockId) ?: return
        val player = event.player

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode quebrar isso.")
            return
        }

        runHook(custom.id) { custom.onBreak(BreakContext(player, block, placed, event)) }
        if (event.isCancelled) return

        // Sem isto o Bukkit dropa também o bloco vanilla e duplica material.
        event.isDropItems = false
        PlacedBlockManager.unregister(placed.location)

        val dropAt = block.location.clone().add(0.5, 0.5, 0.5)
        block.world.dropItemNaturally(dropAt, custom.buildItem(placed))
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        // Shift+clique continua servindo para colocar bloco na face.
        if (player.isSneaking) return

        val block = event.clickedBlock ?: return
        val placed = PlacedBlockManager.get(block) ?: return
        val custom = CustomBlockRegistry.get(placed.blockId) ?: return

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não tem acesso a este bloco.")
            return
        }

        event.isCancelled = true
        runHook(custom.id) {
            custom.onRightClick(BlockClickContext(player, block, placed, viewFrame, event))
        }
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { PlacedBlockManager.get(it) != null }) event.isCancelled = true
    }

    @EventHandler
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { PlacedBlockManager.get(it) != null }) event.isCancelled = true
    }

    /**
     * Tira os blocos registrados da lista da explosão. Deixar explodir sumiria com
     * o bloco físico e manteria a entrada no Mongo — registro fantasma que ocupa
     * slot do limite por ilha e tenta sugar num lugar vazio para sempre.
     */
    private fun protect(blocks: MutableList<Block>) {
        blocks.removeIf { PlacedBlockManager.get(it) != null }
    }

    private fun canManage(island: Island, player: Player): Boolean =
        island.ownerUuid == player.uniqueId || island.isMember(player.name)

    private inline fun runHook(id: String, block: () -> Unit) {
        runCatching { block() }
            .onFailure { Instance.logger.warning("[custom-items] hook de bloco '$id' falhou: ${it.message}") }
    }
}
```

- [ ] **Passo 2: Compilar**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Passo 3: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): listener de ciclo de vida dos blocos custom`.

---

## Task 9: GUIs do Vacuum Hopper

**Files:**
- Create: `content/vacuum_hopper/view/VacuumHopperView.kt`, `.../view/HopperStorageView.kt`, `.../view/HopperFilterView.kt`, `.../view/HopperUpgradesView.kt`
- Modify: `content/vacuum_hopper/VacuumHopper.kt` (mover `clearFullNotice` para o companion e preencher `onRightClick`)

**Interfaces:**
- Consome: `PlacedBlock`, `PlacedBlockManager`, `FilterMode`, `BlockClickContext`; `VacuumHopperUpgrade`; `Currencies` (`one_block.system.currencies`); `NumberFormatter` (`one_block.libs.formatter`).
- Produz: as quatro `View` registráveis no `ViewFrame`, todas com `initialState<PlacedBlock>()`.

- [ ] **Passo 1: Mover `clearFullNotice` para o companion e abrir a view no clique**

Em `content/vacuum_hopper/VacuumHopper.kt`, remover o método de instância:

```kotlin
    /** Chamado quando a GUI abre espaço, para o próximo enchimento avisar de novo. */
    fun clearFullNotice(placed: PlacedBlock) {
        if (!placed.notifiedFull) return
        placed.notifiedFull = false
    }
```

e adicionar, dentro do `companion object`, logo abaixo de `const val ID`:

```kotlin
        /** Chamado quando a GUI abre espaço, para o próximo enchimento avisar de novo. */
        fun clearFullNotice(placed: PlacedBlock) {
            if (!placed.notifiedFull) return
            placed.notifiedFull = false
            PlacedBlockManager.saveNow(placed)
        }
```

No mesmo arquivo, implementar o hook de clique (o import novo é `one_block.system.custom_items.block.context.BlockClickContext` e `one_block.system.custom_items.content.vacuum_hopper.view.VacuumHopperView`):

```kotlin
    override fun onRightClick(ctx: BlockClickContext) {
        ctx.cancel()
        ctx.viewFrame.open(VacuumHopperView::class.java, ctx.player, ctx.placed)
    }
```

- [ ] **Passo 2: Criar `VacuumHopperView`**

`content/vacuum_hopper/view/VacuumHopperView.kt`:

```kotlin
package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopperUpgrade
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class VacuumHopperView : View() {

    private val blockState = initialState<PlacedBlock>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Vacuum Hopper")
        config.layout(
            "         ",
            " I T S U ",
            "         "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placed = blockState.get(render)

        render.layoutSlot('I').renderWith { statusItem(placed) }

        render.layoutSlot('T')
            .renderWith { toggleItem(placed) }
            .onClick { click ->
                placed.enabled = !placed.enabled
                PlacedBlockManager.saveNow(placed)
                click.update()
            }

        render.layoutSlot('S')
            .renderWith { storageItem(placed) }
            .onClick { click -> click.openForPlayer(HopperStorageView::class.java, placed) }

        render.layoutSlot('U')
            .renderWith { upgradesItem() }
            .onClick { click -> click.openForPlayer(HopperUpgradesView::class.java, placed) }
    }

    private fun statusItem(placed: PlacedBlock): ItemStack {
        val capacity = VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))
        val radius = VacuumHopperUpgrade.radiusFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Radius)).toInt()
        val interval = VacuumHopperUpgrade.tickIntervalFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Speed))

        return itemStack(Material.CONDUIT) {
            meta {
                name("<aqua>Vacuum Hopper")
                setLore {
                    +"<dark_gray>▸ <white>Raio: <aqua>$radius blocos"
                    +"<dark_gray>▸ <white>Armazenamento: <aqua>${placed.storage.size}/$capacity"
                    +"<dark_gray>▸ <white>Sucção: <aqua>a cada $interval ticks"
                    +""
                    +if (placed.enabled) "<green>▶ Ligado" else "<red>■ Desligado"
                }
            }
        }
    }

    private fun toggleItem(placed: PlacedBlock) =
        itemStack(if (placed.enabled) Material.LIME_DYE else Material.GRAY_DYE) {
            meta {
                name(if (placed.enabled) "<green>Ligado" else "<red>Desligado")
                setLore {
                    +"<gray>Liga e desliga a sucção."
                    +""
                    +"<yellow>Clique para alternar."
                }
            }
        }

    private fun storageItem(placed: PlacedBlock): ItemStack {
        val capacity = VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))
        return itemStack(Material.CHEST) {
            meta {
                name("<gold>Armazenamento")
                setLore {
                    +"<gray>Itens guardados: <white>${placed.storage.size}/$capacity"
                    +""
                    +"<yellow>Clique para abrir."
                }
            }
        }
    }

    private fun upgradesItem() = itemStack(Material.NETHER_STAR) {
        meta {
            name("<light_purple>Upgrades")
            setLore {
                +"<gray>Evolua raio, armazenamento"
                +"<gray>e velocidade."
                +""
                +"<yellow>Clique para abrir."
            }
        }
    }
}
```

- [ ] **Passo 3: Criar `HopperStorageView`**

`content/vacuum_hopper/view/HopperStorageView.kt`:

```kotlin
package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopper
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopperUpgrade
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class HopperStorageView : View() {

    private val blockState = initialState<PlacedBlock>()

    // Cada slot 'O' mostra um item guardado; clicar retira aquele stack.
    private val pagination = lazyPaginationState(
        { ctx -> blockState.get(ctx).storageItems().toMutableList() }
    ) { _, builder, _, value ->
        builder.renderWith { value }
            .onClick { click -> withdraw(click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Armazenamento")
        config.layout(
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "B  C F   "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('B')
            .renderWith { backItem() }
            .onClick { click -> click.openForPlayer(VacuumHopperView::class.java, blockState.get(click)) }

        render.layoutSlot('C')
            .renderWith { collectAllItem() }
            .onClick { click -> collectAll(click) }

        render.layoutSlot('F')
            .renderWith { filterItem(blockState.get(render)) }
            .onClick { click -> click.openForPlayer(HopperFilterView::class.java, blockState.get(click)) }
    }

    private fun withdraw(click: SlotClickContext, value: ItemStack) {
        val placed = blockState.get(click)
        val stored = placed.storageItems().toMutableList()

        val index = stored.indexOfFirst { it.isSimilar(value) && it.amount == value.amount }
        if (index == -1) return

        val leftover = click.player.inventory.addItem(value.clone())
        if (leftover.isEmpty()) {
            stored.removeAt(index)
        } else {
            // Inventário do jogador encheu: guarda de volta o que sobrou.
            stored[index] = leftover.values.first()
        }

        placed.setStorage(stored)
        VacuumHopper.clearFullNotice(placed)
        PlacedBlockManager.saveNow(placed)
        pagination.get(click).forceUpdate()
    }

    private fun collectAll(click: SlotClickContext) {
        val placed = blockState.get(click)
        val remaining = mutableListOf<ItemStack>()

        placed.storageItems().forEach { item ->
            remaining += click.player.inventory.addItem(item.clone()).values
        }

        placed.setStorage(remaining)
        VacuumHopper.clearFullNotice(placed)
        PlacedBlockManager.saveNow(placed)
        pagination.get(click).forceUpdate()

        if (remaining.isEmpty()) {
            click.player.sendRichMessage("<green>Itens coletados.")
        } else {
            click.player.sendRichMessage("<yellow>Seu inventário encheu; parte dos itens ficou no bloco.")
        }
    }

    private fun backItem() = itemStack(Material.ARROW) {
        meta { name("<gray>← Voltar") }
    }

    private fun collectAllItem() = itemStack(Material.HOPPER) {
        meta {
            name("<green>Coletar tudo")
            setLore {
                +"<gray>Move todos os itens guardados"
                +"<gray>para o seu inventário."
            }
        }
    }

    private fun filterItem(placed: PlacedBlock) = itemStack(Material.COMPARATOR) {
        val capacity = VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))
        meta {
            name("<gold>Filtro")
            setLore {
                +"<gray>Controla quais itens o"
                +"<gray>bloco guarda."
                +""
                +"  <dark_gray>▸ <white>Modo: <aqua>${placed.filterMode.prettyName}"
                +"  <dark_gray>▸ <white>Itens: <aqua>${placed.filter.size}"
                +"  <dark_gray>▸ <white>Capacidade: <aqua>$capacity"
                +""
                +"<yellow>Clique para abrir."
            }
        }
    }
}
```

- [ ] **Passo 4: Criar `HopperFilterView`**

`content/vacuum_hopper/view/HopperFilterView.kt`:

```kotlin
package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.block.FilterMode
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

class HopperFilterView : View() {

    private val blockState = initialState<PlacedBlock>()

    // Cada item do topo é um material da lista; clicar remove.
    private val pagination = lazyPaginationState(
        { ctx ->
            blockState.get(ctx).filter
                .mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
                .toMutableList()
        }
    ) { _, builder, _, material ->
        builder.renderWith { filterEntryItem(material) }
            .onClick { click -> removeFromFilter(click, material) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Filtro do Vacuum Hopper")
        config.layout(
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "         ",
            "B   M   I"
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('B')
            .renderWith { backItem() }
            .onClick { click -> click.openForPlayer(HopperStorageView::class.java, blockState.get(click)) }

        render.layoutSlot('M')
            .renderWith { modeItem(blockState.get(render)) }
            .onClick { click -> cycleMode(click) }

        render.layoutSlot('I').renderWith { infoItem() }
    }

    /**
     * Clique no inventário do próprio jogador adiciona o material à lista. Os
     * cliques no topo são tratados pelos componentes; aqui só interessa o de baixo.
     */
    override fun onClick(click: SlotClickContext) {
        if (click.clickOrigin.clickedInventory?.type != InventoryType.PLAYER) return

        val item = click.clickOrigin.currentItem ?: return
        if (item.type.isAir) return

        val placed = blockState.get(click)
        if (!placed.filter.add(item.type.name)) return

        PlacedBlockManager.saveNow(placed)
        pagination.get(click).forceUpdate()
    }

    private fun removeFromFilter(click: SlotClickContext, material: Material) {
        val placed = blockState.get(click)
        placed.filter.remove(material.name)
        PlacedBlockManager.saveNow(placed)
        pagination.get(click).forceUpdate()
    }

    private fun cycleMode(click: SlotClickContext) {
        val placed = blockState.get(click)
        placed.filterMode = placed.filterMode.next()
        PlacedBlockManager.saveNow(placed)
        pagination.get(click).forceUpdate()
    }

    private fun filterEntryItem(material: Material) = itemStack(material) {
        meta {
            name("<white>${material.name.lowercase().replace('_', ' ')}")
            setLore { +"<red>Clique para remover da lista." }
        }
    }

    private fun modeItem(placed: PlacedBlock): ItemStack {
        val mode = placed.filterMode
        val material = if (mode == FilterMode.WHITELIST) Material.LIME_DYE else Material.RED_DYE
        return itemStack(material) {
            meta {
                name("<yellow>Modo: <white>${mode.prettyName}")
                setLore {
                    +"<gray>${mode.description}"
                    +""
                    FilterMode.entries.forEach { option ->
                        if (option == mode) {
                            +"<green>▸ <white>${option.prettyName}"
                        } else {
                            +"<dark_gray>  ${option.prettyName}"
                        }
                    }
                    +""
                    +"<yellow>Clique para alternar."
                }
            }
        }
    }

    private fun infoItem() = itemStack(Material.BOOK) {
        meta {
            name("<aqua>Como usar")
            setLore {
                +"<gray>Clique num item do seu"
                +"<gray>inventário para adicioná-lo"
                +"<gray>à lista de filtro."
                +""
                +"<gray>Clique num item do topo"
                +"<gray>para removê-lo."
            }
        }
    }

    private fun backItem() = itemStack(Material.ARROW) {
        meta { name("<gray>← Voltar") }
    }
}
```

- [ ] **Passo 5: Criar `HopperUpgradesView`**

`content/vacuum_hopper/view/HopperUpgradesView.kt`:

```kotlin
package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.currencies.Currencies
import one_block.system.custom_items.block.BlockUpgrade
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopperUpgrade
import org.bukkit.Material
import org.bukkit.Sound

class HopperUpgradesView : View() {

    private val blockState = initialState<PlacedBlock>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Upgrades")
        config.layout(
            "         ",
            "B        ",
            "         "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placed = blockState.get(render)

        render.layoutSlot('B')
            .renderWith { itemStack(Material.ARROW) { meta { name("<gray>← Voltar") } } }
            .onClick { click -> click.openForPlayer(VacuumHopperView::class.java, placed) }

        VacuumHopperUpgrade.entries.forEachIndexed { index, upgrade ->
            render.slot(1, 3 + index)
                .renderWith { upgradeItem(placed, upgrade) }
                .onClick { click -> buy(click, upgrade) }
        }
    }

    private fun upgradeItem(placed: PlacedBlock, upgrade: BlockUpgrade) = itemStack(
        if (placed.getUpgradeLevel(upgrade) >= upgrade.maxLevel) Material.NETHER_STAR else Material.FEATHER
    ) {
        val level = placed.getUpgradeLevel(upgrade)
        val atMax = level >= upgrade.maxLevel
        val stars = "<gold>${"★".repeat(level)}<dark_gray>${"★".repeat(upgrade.maxLevel - level)}"

        meta {
            name(if (atMax) "<gold>${upgrade.prettyName}" else "<yellow>${upgrade.prettyName}")
            setLore {
                +"<dark_gray>──────────────"
                +"$stars <dark_gray>($level/${upgrade.maxLevel})"
                +""
                upgrade.lore.forEach { +"<dark_gray>$it" }
                +""
                +"<white>Atual: ${upgrade.statLine(level)}"
                if (!atMax) {
                    +"<white>Próximo: ${upgrade.statLine(level + 1)}"
                    +""
                    +"<gray>Custo: <gold>${NumberFormatter.format(upgrade.costFor(level))} coins"
                    +"<gray>Clique para evoluir."
                } else {
                    +""
                    +"<gold>✦ Nível máximo atingido!"
                }
                +"<dark_gray>──────────────"
            }
        }
    }

    private fun buy(click: SlotClickContext, upgrade: BlockUpgrade) {
        val placed = blockState.get(click)
        val player = click.player
        val level = placed.getUpgradeLevel(upgrade)

        if (level >= upgrade.maxLevel) {
            player.sendRichMessage("<red>Esse upgrade já está no nível máximo!")
            return
        }

        val cost = upgrade.costFor(level)
        if (!Currencies.has(player.uniqueId, VacuumHopperUpgrade.UPGRADE_CURRENCY, cost)) {
            player.sendRichMessage("<red>Você precisa de <white>${NumberFormatter.format(cost)} coins<red> para evoluir.")
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        // Débito e nível numa operação só, persistindo em seguida.
        Currencies.withdraw(player.uniqueId, VacuumHopperUpgrade.UPGRADE_CURRENCY, cost)
        placed.setUpgradeLevel(upgrade, level + 1)
        PlacedBlockManager.saveNow(placed)

        player.sendRichMessage("<green>${upgrade.prettyName} evoluído para o nível <white>${level + 1}<green>!")
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)

        click.openForPlayer(HopperUpgradesView::class.java, placed)
    }
}
```

- [ ] **Passo 6: Compilar**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`. As views ainda não estão registradas em `ViewFrame` — isso é a Task 10.

- [ ] **Passo 7: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): guis do vacuum hopper`.

---

## Task 10: Comando, wiring e receita

**Files:**
- Create: `block/CustomBlockCommand.kt`
- Modify: `CustomItemSystem.kt` (arquivo inteiro abaixo)
- Modify: `plugins/oneblock/plugin/src/main/kotlin/io/github/badnotice/one_block/OneBlockPlugin.kt`

**Interfaces:**
- Consome: tudo das tarefas anteriores; `Crafts` (`one_block.system.craft`).
- Produz: `class CustomBlockCommand : BaseCommand()` com `/customblock give <id> [player] [qtd]` e `/customblock list`; `CustomItemSystem.init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager)`.

`ForgeRecipeManager.addRecipe` só faz append numa lista, então registrar a receita antes de `CraftSystem.init` é seguro — não é preciso reordenar `setupSystems()`.

- [ ] **Passo 1: Criar `CustomBlockCommand`**

`block/CustomBlockCommand.kt`:

```kotlin
package one_block.system.custom_items.block

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("customblock|cblock")
@CommandPermission("screen.dev")
class CustomBlockCommand : BaseCommand() {

    @Subcommand("give")
    @CommandCompletion("@customblocks @players @range:1-64")
    fun give(
        sender: Player,
        id: String,
        @Optional target: OnlinePlayer?,
        @Default("1") @Optional amount: Int,
    ) {
        val block = CustomBlockRegistry.get(id)
        if (block == null) {
            sender.sendRichMessage("<red>Bloco desconhecido: <white>$id")
            return
        }

        val receiver = target?.player ?: sender
        val amt = amount.coerceIn(1, 64)
        val overflow = receiver.inventory.addItem(block.buildItem(amount = amt))

        overflow.values.forEach { receiver.world.dropItemNaturally(receiver.location, it) }
        sender.sendRichMessage("<green>Entregue <white>${amt}x $id<green> para <white>${receiver.name}")
    }

    @Subcommand("list")
    fun list(sender: CommandSender) {
        val ids = CustomBlockRegistry.ids()
        if (ids.isEmpty()) {
            sender.sendRichMessage("<gray>Nenhum bloco custom registrado.")
            return
        }
        sender.sendRichMessage("<yellow>Blocos custom (${ids.size}): <white>${ids.sorted().joinToString(", ")}")
    }
}
```

- [ ] **Passo 2: Reescrever `CustomItemSystem`**

Substituir todo o conteúdo de `CustomItemSystem.kt` por:

```kotlin
package one_block.system.custom_items

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.craft.Crafts
import one_block.system.custom_items.block.CustomBlockCommand
import one_block.system.custom_items.block.CustomBlockListener
import one_block.system.custom_items.block.CustomBlockRegistry
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.magnet.MagnetItem
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopper
import one_block.system.custom_items.content.vacuum_hopper.view.HopperFilterView
import one_block.system.custom_items.content.vacuum_hopper.view.HopperStorageView
import one_block.system.custom_items.content.vacuum_hopper.view.HopperUpgradesView
import one_block.system.custom_items.content.vacuum_hopper.view.VacuumHopperView
import one_block.system.custom_items.item.CustomItemListener
import one_block.system.custom_items.item.CustomItemRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * Wiring dos dois eixos do módulo: itens em mão (`item/`) e blocos colocados
 * (`block/`). O conteúdo de jogo de cada eixo mora em `content/`.
 */
object CustomItemSystem {

    lateinit var plugin: JavaPlugin
    private var tickCounter = 0L

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        this.plugin = plugin

        registerCompletions(commandManager)
        plugin.server.pluginManager.registerEvents(CustomItemListener(), plugin)
        plugin.server.pluginManager.registerEvents(CustomBlockListener(viewFrame), plugin)
        startHeldTickTask()

        registerBuiltInItems()
        registerBuiltInBlocks(viewFrame, commandManager)

        PlacedBlockManager.onInit()
    }

    private fun registerCompletions(commandManager: PaperCommandManager) {
        commandManager.commandCompletions.registerCompletion("customitems") {
            CustomItemRegistry.ids()
        }
        commandManager.commandCompletions.registerCompletion("customblocks") {
            CustomBlockRegistry.ids()
        }
    }

    private fun startHeldTickTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            tickCounter++
            for (player in Bukkit.getOnlinePlayers()) {
                tickHand(player, player.inventory.itemInMainHand)
                tickHand(player, player.inventory.itemInOffHand)
            }
        }, 1L, 1L)
    }

    private fun tickHand(player: Player, item: ItemStack) {
        val custom = CustomItemRegistry.fromItemStack(item) ?: return
        if (custom.heldTickInterval <= 0L) return
        if (tickCounter % custom.heldTickInterval != 0L) return

        runCatching { custom.onHeldTick(player, item) }
            .onFailure { plugin.logger.warning("[custom-items] held-tick de '${custom.id}' falhou: ${it.message}") }
    }

    private fun registerBuiltInItems() {
        CustomItemRegistry.register(MagnetItem())
    }

    // Comando e completion do eixo bloco ficam juntos aqui, ao contrário do eixo
    // item, cujo comando ainda é registrado pelo OneBlockPlugin.
    private fun registerBuiltInBlocks(viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        val hopper = VacuumHopper()
        CustomBlockRegistry.register(hopper)

        viewFrame.with(
            VacuumHopperView(),
            HopperStorageView(),
            HopperFilterView(),
            HopperUpgradesView(),
        )

        commandManager.registerCommand(CustomBlockCommand())
        Crafts.register(VacuumHopper.forgeRecipe(hopper))
    }
}
```

- [ ] **Passo 3: Atualizar o `OneBlockPlugin`**

Em `OneBlockPlugin.kt`, adicionar o import:

```kotlin
import one_block.system.custom_items.block.PlacedBlockManager
```

Trocar a chamada em `setupSystems()`:

```kotlin
            { CustomItemSystem.init(this, paperCommandManager) },
```

por:

```kotlin
            { CustomItemSystem.init(this, viewFrame, paperCommandManager) },
```

E no `onDisable()`, acrescentar a linha (managers com estado em memória precisam ser chamados explicitamente aqui — nada faz isso sozinho):

```kotlin
    override fun onDisable() {
        CoinFlipManager.onDisable()
        PlacedMinionManager.onDisable()
        PlacedBlockManager.onDisable()
    }
```

- [ ] **Passo 4: Compilar o plugin inteiro**

```bash
./gradlew :plugins:oneblock:plugin:localJar
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Passo 5: Rodar a suíte do módulo**

```bash
./gradlew :plugins:oneblock:system:custom-items:localTest
```

Esperado: `BUILD SUCCESSFUL`, 22 testes (5 `CooldownStore`, 4 `FilterRules`, 6 `StateCodec`, 7 `VacuumHopperUpgrade`).

- [ ] **Passo 6: Checkpoint**

Peça o commit. Sugestão: `feat(custom-items): comando de blocos, wiring e receita do vacuum hopper`.

---

## Task 11: Verificação manual no servidor

**Files:** nenhum. Esta tarefa é execução e observação.

O que não dá para testar em unidade (tudo que depende de `Bukkit.createInventory`, `ItemSerializer` e do `PersistentDataContainer`) é verificado aqui.

- [ ] **Passo 1: Deploy**

Pare o servidor de desenvolvimento (o Windows tranca o jar em uso), depois:

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Suba o servidor e confirme no console: `[custom-items] N bloco(s) custom carregado(s).`

- [ ] **Passo 2: Obtenção**

- `/customblock list` → lista `vacuum_hopper`.
- `/customblock give vacuum_hopper` → recebe o item com o nome `Vacuum Hopper`.
- Abrir a Forge e conferir a receita: 1 Conduit + 4 Barra de Netherite + 8 Peça Mecânica produz o bloco.

- [ ] **Passo 3: Colocar, quebrar e preservar estado**

- Colocar o bloco na sua ilha → mensagem `Bloco colocado. (1/5)`.
- Jogar itens perto e esperar a sucção; abrir a GUI e ver os itens no armazenamento.
- Quebrar o bloco → nenhum conduit vanilla dropa junto; o item dropado mostra `Itens guardados: N` na lore.
- Recolocar → abrir a GUI e conferir que armazenamento, filtro e upgrades voltaram iguais.

- [ ] **Passo 4: Limite por ilha e permissão**

- Colocar 5 blocos na mesma ilha; o sexto é bloqueado com `Limite de 5 por ilha atingido.`
- Com outro jogador **não** membro da ilha: colocar, quebrar e clicar devem ser negados.
- Com um jogador membro: as três ações devem funcionar.

- [ ] **Passo 5: Registro fantasma**

- Detonar TNT ao lado de um bloco colocado → o bloco continua de pé.
- Empurrar o bloco com pistão → o pistão não se move.
- Substituir o bloco por outro material via `/setblock` e esperar o próximo tick → console loga `bloco 'vacuum_hopper' sumiu em ...; registro removido.` e o slot volta a contar para o limite.

- [ ] **Passo 6: Sucção, filtro e cheio**

- Filtro em `Filtrar` (blacklist) com `COBBLESTONE`: cobblestone fica no chão, o resto é sugado.
- Alternar para `Aceitar` (whitelist): só cobblestone é sugado.
- Encher o armazenamento → aviso único ao dono (`está cheio e parou de sugar`), sem repetir a cada tick; retirar um item pela GUI e encher de novo → o aviso volta a aparecer uma vez.
- Deixar um stack parcial caber: o drop no chão deve ficar com a quantidade restante, não sumir.

- [ ] **Passo 7: Upgrades**

- Comprar cada upgrade e conferir o débito em `coins` (`/saldo` ou equivalente).
- Sem saldo: mensagem de erro e nenhum débito.
- Nível 3 de Armazenamento → GUI abre com 54 slots sem erro no console.
- Nível 3 de Velocidade → sucção visivelmente mais rápida.

- [ ] **Passo 8: Persistência**

- Com itens no armazenamento, reiniciar o servidor e conferir que o conteúdo voltou (flush em lote a cada 10s + `onDisable`).
- Comprar um upgrade e derrubar o servidor logo em seguida (kill) → o upgrade sobrevive, porque compra usa `saveNow`.

- [ ] **Passo 9: Checkpoint final**

Relate ao usuário o que passou e o que falhou. Nada de commit automático.

---

## Cobertura do design

| Seção do design | Onde entra |
|---|---|
| Estrutura de pacotes / reorganização | Task 1 |
| `FilterMode`, filtro, serialização | Tasks 2 e 4 |
| Upgrades, custos, teto de 54 slots | Task 3 |
| `PlacedBlock`, repositório, coleção `custom_blocks` | Task 4 |
| API `CustomBlock`, `BlockUpgrade`, estado no PDC | Tasks 2, 4 e 5 |
| `PlacedBlockCodec` separado | Task 5 |
| Manager, tick global, validação preguiçosa, flush em lote | Task 6 |
| Sucção, aviso de cheio | Task 7 |
| Colocar / quebrar / interagir / explosão / pistão | Task 8 |
| Quatro GUIs | Task 9 |
| `CustomBlockCommand` genérico, receita na Forge, wiring | Task 10 |
| Verificação manual listada no design | Task 11 |

Fora de escopo, conforme o design: sugar XP, puxar de containers, empurrar para container, tiers de item, teto compartilhado com minions, configuração por YAML.
