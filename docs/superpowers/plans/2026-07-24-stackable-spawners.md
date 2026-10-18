# Spawners Stackáveis — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir o módulo `plugins/oneblock/system/spawners`: spawner colocável e empilhável que gera mobs stackados numa única entidade sem IA, descascada golpe a golpe pelo jogador.

**Architecture:** Módulo independente que espelha o padrão Manager + Repository do projeto. A aritmética vive em `rules/`, sem estado; listeners traduzem evento do Bukkit em chamada de regra e o manager guarda o resultado. A entidade viva é descartável — a fonte de verdade é o campo `liveMobs` no `PlacedSpawner`, persistido no Mongo.

**Tech Stack:** Kotlin 2.3.0, JDK 21, Paper 1.21.8, Aikar ACF, inventory-framework, MongoDB (codec POJO).

**Spec:** `docs/superpowers/specs/2026-07-24-stackable-spawners-design.md`

## Global Constraints

- **Sem testes automatizados.** Decisão do usuário. O módulo não adiciona Kotest nem `tasks.test`. A verificação é o roteiro de servidor no fim deste plano, e ele **não é opcional** — é a única rede do sistema.
- **Comentários e strings de usuário em português.** Convenção do repositório.
- **Nunca rodar `./gradlew build` ou `compileKotlin` direto.** O modo do build vem do nome da task conter `local` (ver CLAUDE.md); qualquer outra coisa tenta resolver `com.redescreen:sdk:1.0`, que só existe em produção. O módulo registra um alias `localCompile` para compilar isolado.
- **Deploy exige servidor parado.** `localDeploy` copia o jar para `server/plugins/`, e o Windows trava o arquivo em uso.
- **Texto com MiniMessage via `Mini.mm`** (`one_block.libs.formatter.Mini`), nunca `MiniMessage.miniMessage()` — `Mini.mm` carrega as tags do Textura. Para chat simples, `player.sendRichMessage(...)` segue o padrão já usado no projeto.
- **`ItemStack` nunca vai como objeto aninhado no Mongo.** Não se aplica aqui — `PlacedSpawner` não tem campo de item — mas vale se um aparecer: base64 via `ItemSerializer` de `lib:item-builder`.
- **Commits são do usuário.** Os passos de commit indicam o ponto de corte e a mensagem sugerida; confirme com o usuário antes de commitar.
- **Distância mínima entre spawners:** 5.0 blocos, euclidiana 3D.
- **Teto de `stackSize` por bloco:** 128.
- **Moeda da loja:** `"coins"`.

## Desvio consciente do spec

**A entidade não usa `setInvulnerable(true)`.** O spec diz "invulnerável", mas essa flag bloquearia também o dano do jogador — o mob ficaria imortal e o loot inalcançável. A imunidade a ambiente vem de um handler de `EntityDamageEvent` que cancela toda causa que não seja golpe de jogador (Task 9). O spec será atualizado.

## Estrutura de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `build.gradle.kts` | Dependências e alias `localCompile` |
| `spawner/SpawnerTier.kt` | Enum de tier |
| `spawner/LootEntry.kt` | Uma linha de tabela de loot |
| `spawner/SpawnerType.kt` | Enum com a config inteira do sistema |
| `spawner/PlacedSpawner.kt` | Estado persistido |
| `spawner/PlacedSpawnerRepository.kt` | Acesso ao Mongo |
| `spawner/PlacedSpawnerManager.kt` | Fonte de verdade em runtime, tick, flush |
| `spawner/SpawnerKeys.kt` | Chaves de PDC |
| `spawner/SpawnerItems.kt` | Item do spawner |
| `rules/PlacementRules.kt` | Distância mínima |
| `rules/StackRules.kt` | Empilhar/retirar com teto |
| `rules/SpawnRules.kt` | Incremento/decremento de `liveMobs` |
| `rules/LootRoll.kt` | Sorteio da tabela |
| `entity/SpawnerEntity.kt` | Criar/atualizar/remover a entidade stackada |
| `entity/SpawnerDisplay.kt` | `TextDisplay` acima do bloco |
| `listener/SpawnerBlockListener.kt` | Colocar, empilhar, quebrar, explosão, pistão |
| `listener/SpawnerDeathListener.kt` | Dano, morte, loot |
| `view/SpawnerView.kt` | GUI do spawner |
| `view/SpawnerShopView.kt` | Loja |
| `command/SpawnerCommand.kt` | `/spawner give`, `/spawner loja` |
| `SpawnersSystem.kt` | Wiring |

---

### Task 1: Scaffold do módulo

**Files:**
- Create: `plugins/oneblock/system/spawners/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `.../spawners/src/main/kotlin/one_block/system/spawners/spawner/SpawnerTier.kt`

**Interfaces:**
- Consumes: nada
- Produces: `SpawnerTier` (enum `I`, `II`, `III`, com `prettyName: String`)

- [ ] **Step 1: Registrar o módulo no settings**

Em `settings.gradle.kts`, dentro do `include(...)`, junto das outras linhas `:plugins:oneblock:system:`:

```kotlin
    ":plugins:oneblock:system:spawners",
```

- [ ] **Step 2: Criar o build.gradle.kts**

`plugins/oneblock/system/spawners/build.gradle.kts`:

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
    compileOnly(projects.plugins.oneblock.system.currencies)
}

// O modo do build vem do nome da task conter "local" (ver CLAUDE.md). Um
// `compileKotlin` direto resolveria `com.redescreen:sdk`, que só existe em
// produção — daí este alias para compilar o módulo isolado durante o desenvolvimento.
tasks.register("localCompile") {
    group = "build"
    description = "Compila o módulo em modo local."
    dependsOn(tasks.named("compileKotlin"))
}
```

- [ ] **Step 3: Criar o enum de tier**

`.../spawners/spawner/SpawnerTier.kt`:

```kotlin
package one_block.system.spawners.spawner

/** Agrupamento de raridade dos spawners. Só afeta apresentação e preço. */
enum class SpawnerTier(val prettyName: String) {
    I("Tier I"),
    II("Tier II"),
    III("Tier III"),
}
```

- [ ] **Step 4: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`. Confirma que o módulo foi registrado e resolve dependências.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts plugins/oneblock/system/spawners/
git commit -m "feat(spawners): scaffold do módulo"
```

---

### Task 2: Regras puras

**Files:**
- Create: `.../spawners/rules/PlacementRules.kt`
- Create: `.../spawners/rules/StackRules.kt`
- Create: `.../spawners/rules/SpawnRules.kt`

**Interfaces:**
- Consumes: nada
- Produces:
  - `PlacementRules.MIN_DISTANCE: Double` (= 5.0); `PlacementRules.isTooClose(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double, minDistance: Double = MIN_DISTANCE): Boolean`
  - `StackRules.MAX_STACK: Int` (= 128); `StackRules.StackResult(newStack: Int, consumed: Int, leftover: Int)`; `StackRules.add(current: Int, offered: Int, max: Int = MAX_STACK): StackResult`; `StackRules.WithdrawResult(newStack: Int, withdrawn: Int)`; `StackRules.withdraw(current: Int, requested: Int, inventorySpace: Int): WithdrawResult`
  - `SpawnRules.shouldSpawn(enabled: Boolean, current: Int, maxLive: Int): Boolean`; `SpawnRules.nextLiveMobs(current: Int, stackSize: Int, maxLive: Int): Int`; `SpawnRules.afterKill(current: Int): Int`

Estas três funções carregam todo o balanço do sistema e nenhuma tem teste. Os
comentários de contrato existem por isso — são o que sobra para o próximo leitor
conferir se o comportamento bate com a intenção.

- [ ] **Step 1: PlacementRules**

`.../spawners/rules/PlacementRules.kt`:

```kotlin
package one_block.system.spawners.rules

/**
 * Regra de espaçamento entre spawners.
 *
 * Recebe coordenadas soltas em vez de `SimpleLocation` para manter a regra
 * independente de tipo de mundo — quem chama extrai os campos. A comparação de
 * mundo é responsabilidade do chamador.
 */
object PlacementRules {

    /** Distância euclidiana mínima, em blocos, entre dois spawners. */
    const val MIN_DISTANCE = 5.0

    /**
     * `true` se os dois pontos estão perto demais para abrigar spawners.
     *
     * Contrato: a distância é euclidiana em 3D, então a altura conta como
     * qualquer outro eixo — dois spawners empilhados verticalmente a 4 blocos são
     * recusados. Exatamente [minDistance] é permitido. Compara o quadrado das
     * distâncias para não pagar a raiz.
     */
    fun isTooClose(
        ax: Double, ay: Double, az: Double,
        bx: Double, by: Double, bz: Double,
        minDistance: Double = MIN_DISTANCE,
    ): Boolean {
        val dx = ax - bx
        val dy = ay - by
        val dz = az - bz
        return (dx * dx + dy * dy + dz * dz) < (minDistance * minDistance)
    }
}
```

- [ ] **Step 2: StackRules**

`.../spawners/rules/StackRules.kt`:

```kotlin
package one_block.system.spawners.rules

/**
 * Aritmética de empilhar e retirar spawners de um bloco. Sem efeito colateral:
 * quem chama é responsável por consumir itens da mão e devolver ao inventário
 * conforme o resultado.
 */
object StackRules {

    /** Quantos spawners cabem em um único bloco. */
    const val MAX_STACK = 128

    /**
     * @property newStack novo tamanho do stack do bloco
     * @property consumed quantos itens devem sair da mão do jogador
     * @property leftover quantos sobraram por não caberem no teto
     */
    data class StackResult(val newStack: Int, val consumed: Int, val leftover: Int)

    /**
     * Empilha [offered] no bloco, limitado por [max].
     *
     * Contrato: `consumed + leftover == offered` sempre, e `newStack` nunca passa
     * de [max]. Oferta zero ou negativa não muda nada e não consome.
     */
    fun add(current: Int, offered: Int, max: Int = MAX_STACK): StackResult {
        if (offered <= 0) return StackResult(current, consumed = 0, leftover = maxOf(offered, 0))

        val space = (max - current).coerceAtLeast(0)
        val consumed = minOf(space, offered)
        return StackResult(
            newStack = current + consumed,
            consumed = consumed,
            leftover = offered - consumed,
        )
    }

    /**
     * @property newStack novo tamanho do stack do bloco
     * @property withdrawn quantos itens devem entrar no inventário do jogador
     */
    data class WithdrawResult(val newStack: Int, val withdrawn: Int)

    /**
     * Retira até [requested], limitado pelo que existe no bloco e pelo
     * [inventorySpace] livre.
     *
     * Contrato: retirada parcial é o caminho normal, não erro — `withdrawn` é o
     * menor entre os três valores. Sem espaço, devolve zero e não altera o stack.
     */
    fun withdraw(current: Int, requested: Int, inventorySpace: Int): WithdrawResult {
        if (requested <= 0 || inventorySpace <= 0) return WithdrawResult(current, withdrawn = 0)

        val withdrawn = minOf(current, requested, inventorySpace)
        return WithdrawResult(newStack = current - withdrawn, withdrawn = withdrawn)
    }
}
```

- [ ] **Step 3: SpawnRules**

`.../spawners/rules/SpawnRules.kt`:

```kotlin
package one_block.system.spawners.rules

/** Aritmética do contador de mobs vivos de um spawner. */
object SpawnRules {

    /** `true` se o tick deve rodar um ciclo de spawn agora. */
    fun shouldSpawn(enabled: Boolean, current: Int, maxLive: Int): Boolean =
        enabled && current < maxLive

    /**
     * Um ciclo de spawn: soma o stack inteiro de uma vez, cortado pelo teto.
     *
     * Contrato: é o stack que define quantos mobs nascem por ciclo — um spawner
     * x64 põe 64 mobs. O resultado nunca passa de [maxLive], então perto do teto
     * o ciclo entrega menos que o stack, e isso é intencional.
     */
    fun nextLiveMobs(current: Int, stackSize: Int, maxLive: Int): Int =
        (current + stackSize).coerceAtMost(maxLive)

    /**
     * Uma morte por golpe de jogador descasca exatamente um mob, nunca abaixo de
     * zero.
     */
    fun afterKill(current: Int): Int = (current - 1).coerceAtLeast(0)
}
```

- [ ] **Step 4: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): regras de posicionamento, stack e spawn"
```

---

### Task 3: Tabela de loot e configuração dos tipos

**Files:**
- Create: `.../spawners/spawner/LootEntry.kt`
- Create: `.../spawners/rules/LootRoll.kt`
- Create: `.../spawners/spawner/SpawnerType.kt`

**Interfaces:**
- Consumes: `SpawnerTier` (Task 1)
- Produces:
  - `LootEntry(material: Material, min: Int, max: Int, chance: Double)`
  - `LootRoll.roll(table: List<LootEntry>, random: Random = Random.Default): List<ItemStack>`
  - `SpawnerType` (enum) com `entityType: EntityType`, `tier: SpawnerTier`, `displayName: String`, `price: Double`, `spawnIntervalTicks: Long`, `maxLiveMobs: Int`, `xpPerKill: Int`, `loot: List<LootEntry>`; e `SpawnerType.find(id: String): SpawnerType?`

- [ ] **Step 1: LootEntry**

`.../spawners/spawner/LootEntry.kt`:

```kotlin
package one_block.system.spawners.spawner

import org.bukkit.Material

/**
 * Uma linha da tabela de loot de um [SpawnerType].
 *
 * @property chance probabilidade de 0.0 a 1.0, sorteada de forma independente
 *   das outras entradas.
 */
data class LootEntry(
    val material: Material,
    val min: Int,
    val max: Int,
    val chance: Double,
)
```

- [ ] **Step 2: LootRoll**

`.../spawners/rules/LootRoll.kt`:

```kotlin
package one_block.system.spawners.rules

import one_block.system.spawners.spawner.LootEntry
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Sorteio da tabela de loot de uma morte.
 *
 * O [Random] é parâmetro, e não `Random.Default` fixo por dentro, para o
 * resultado ser reproduzível quando for preciso investigar um drop suspeito.
 */
object LootRoll {

    /**
     * Sorteia cada entrada de forma independente e devolve só as que caíram.
     *
     * Contrato: `chance = 1.0` sempre cai, `0.0` nunca; a quantidade fica em
     * `min..max` inclusive, e `max <= min` devolve `min`.
     */
    fun roll(table: List<LootEntry>, random: Random = Random.Default): List<ItemStack> =
        table.mapNotNull { entry ->
            if (random.nextDouble() >= entry.chance) return@mapNotNull null

            val amount = if (entry.max <= entry.min) {
                entry.min
            } else {
                random.nextInt(entry.min, entry.max + 1)
            }
            ItemStack(entry.material, amount)
        }
}
```

- [ ] **Step 3: SpawnerType**

`.../spawners/spawner/SpawnerType.kt`:

```kotlin
package one_block.system.spawners.spawner

import org.bukkit.Material
import org.bukkit.entity.EntityType

/**
 * Configuração completa dos spawners — em código, por decisão de design (ver
 * spec). Cada valor é um mob suportado; o `name` do enum é o `typeId` persistido
 * em [PlacedSpawner], então **renomear um valor órfã os spawners já colocados**.
 *
 * Mobs deixados de fora de propósito: Slime e Magma Cube (dividem ao morrer e
 * brigam com o modelo de stack), Ghast (hitbox grande demais parada sobre o
 * bloco) e Guardian (precisa de água).
 */
enum class SpawnerType(
    val entityType: EntityType,
    val tier: SpawnerTier,
    val displayName: String,
    val price: Double,
    val spawnIntervalTicks: Long,
    val maxLiveMobs: Int,
    val xpPerKill: Int,
    val loot: List<LootEntry>,
) {

    ZOMBIE(
        entityType = EntityType.ZOMBIE,
        tier = SpawnerTier.I,
        displayName = "<green>Spawner de Zumbi",
        price = 5_000.0,
        spawnIntervalTicks = 200L,
        maxLiveMobs = 500,
        xpPerKill = 5,
        loot = listOf(
            LootEntry(Material.ROTTEN_FLESH, min = 1, max = 2, chance = 1.0),
            LootEntry(Material.IRON_INGOT, min = 1, max = 1, chance = 0.03),
        ),
    ),

    SKELETON(
        entityType = EntityType.SKELETON,
        tier = SpawnerTier.I,
        displayName = "<gray>Spawner de Esqueleto",
        price = 5_000.0,
        spawnIntervalTicks = 200L,
        maxLiveMobs = 500,
        xpPerKill = 5,
        loot = listOf(
            LootEntry(Material.BONE, min = 1, max = 2, chance = 1.0),
            LootEntry(Material.ARROW, min = 1, max = 2, chance = 0.5),
        ),
    ),

    SPIDER(
        entityType = EntityType.SPIDER,
        tier = SpawnerTier.I,
        displayName = "<dark_red>Spawner de Aranha",
        price = 5_000.0,
        spawnIntervalTicks = 200L,
        maxLiveMobs = 500,
        xpPerKill = 5,
        loot = listOf(
            LootEntry(Material.STRING, min = 1, max = 2, chance = 1.0),
            LootEntry(Material.SPIDER_EYE, min = 1, max = 1, chance = 0.33),
        ),
    ),

    CREEPER(
        entityType = EntityType.CREEPER,
        tier = SpawnerTier.II,
        displayName = "<green>Spawner de Creeper",
        price = 25_000.0,
        spawnIntervalTicks = 300L,
        maxLiveMobs = 400,
        xpPerKill = 8,
        loot = listOf(
            LootEntry(Material.GUNPOWDER, min = 1, max = 2, chance = 1.0),
        ),
    ),

    ENDERMAN(
        entityType = EntityType.ENDERMAN,
        tier = SpawnerTier.II,
        displayName = "<dark_purple>Spawner de Enderman",
        price = 25_000.0,
        spawnIntervalTicks = 300L,
        maxLiveMobs = 400,
        xpPerKill = 8,
        loot = listOf(
            LootEntry(Material.ENDER_PEARL, min = 1, max = 1, chance = 0.5),
        ),
    ),

    ZOMBIFIED_PIGLIN(
        entityType = EntityType.ZOMBIFIED_PIGLIN,
        tier = SpawnerTier.II,
        displayName = "<gold>Spawner de Zumbi Pigman",
        price = 25_000.0,
        spawnIntervalTicks = 300L,
        maxLiveMobs = 400,
        xpPerKill = 8,
        loot = listOf(
            LootEntry(Material.ROTTEN_FLESH, min = 1, max = 1, chance = 1.0),
            LootEntry(Material.GOLD_NUGGET, min = 1, max = 3, chance = 1.0),
            LootEntry(Material.GOLD_INGOT, min = 1, max = 1, chance = 0.05),
        ),
    ),

    BLAZE(
        entityType = EntityType.BLAZE,
        tier = SpawnerTier.III,
        displayName = "<yellow>Spawner de Blaze",
        price = 100_000.0,
        spawnIntervalTicks = 400L,
        maxLiveMobs = 300,
        xpPerKill = 12,
        loot = listOf(
            LootEntry(Material.BLAZE_ROD, min = 1, max = 1, chance = 1.0),
        ),
    ),

    WITHER_SKELETON(
        entityType = EntityType.WITHER_SKELETON,
        tier = SpawnerTier.III,
        displayName = "<dark_gray>Spawner de Esqueleto Wither",
        price = 100_000.0,
        spawnIntervalTicks = 400L,
        maxLiveMobs = 300,
        xpPerKill = 12,
        loot = listOf(
            LootEntry(Material.COAL, min = 1, max = 1, chance = 1.0),
            LootEntry(Material.BONE, min = 1, max = 2, chance = 1.0),
            LootEntry(Material.WITHER_SKELETON_SKULL, min = 1, max = 1, chance = 0.025),
        ),
    ),

    WITCH(
        entityType = EntityType.WITCH,
        tier = SpawnerTier.III,
        displayName = "<light_purple>Spawner de Bruxa",
        price = 100_000.0,
        spawnIntervalTicks = 400L,
        maxLiveMobs = 300,
        xpPerKill = 12,
        loot = listOf(
            LootEntry(Material.REDSTONE, min = 1, max = 3, chance = 0.5),
            LootEntry(Material.GLOWSTONE_DUST, min = 1, max = 3, chance = 0.5),
            LootEntry(Material.GLASS_BOTTLE, min = 1, max = 1, chance = 0.25),
        ),
    ),
    ;

    companion object {
        /** Resolve pelo `typeId` persistido. `null` se o valor não existe mais. */
        fun find(id: String): SpawnerType? = entries.firstOrNull { it.name == id }
    }
}
```

- [ ] **Step 4: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`. Se algum `Material` ou `EntityType` não existir na versão do Paper em uso, o erro aparece aqui.

- [ ] **Step 5: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): tabela de loot e configuração dos tipos"
```

---

### Task 4: Modelo e repositório

**Files:**
- Create: `.../spawners/spawner/PlacedSpawner.kt`
- Create: `.../spawners/spawner/PlacedSpawnerRepository.kt`

**Interfaces:**
- Consumes: `SpawnerType.find` (Task 3)
- Produces:
  - `PlacedSpawner(uuid: UUID, location: SimpleLocation, typeId: String, ownerUuid: UUID, stackSize: Int, liveMobs: Int, enabled: Boolean)` com `fun type(): SpawnerType?`
  - `PlacedSpawnerRepository` com `toList()`, `update(PlacedSpawner)`, `updateAll(List<PlacedSpawner>)`, `remove(UUID)`

- [ ] **Step 1: PlacedSpawner**

`.../spawners/spawner/PlacedSpawner.kt`:

```kotlin
package one_block.system.spawners.spawner

import one_block.libs.coordinate.SimpleLocation
import java.util.UUID

/**
 * Estado de um spawner colocado no mundo, persistido na coleção `spawners`.
 *
 * [liveMobs] é a fonte de verdade dos mobs vivos: a entidade no mundo é
 * descartável e recriada a partir deste número sempre que o chunk carrega. Isso
 * é o que faz o acúmulo sobreviver a restart e o que fecha dupe — só o caminho
 * de morte por jogador mexe neste campo.
 *
 * Sem campo de `ItemStack`: o codec POJO do Mongo dá conta de todos os tipos
 * daqui direto.
 */
data class PlacedSpawner(
    val uuid: UUID,
    val location: SimpleLocation,
    /** `name` do [SpawnerType]. */
    val typeId: String,
    val ownerUuid: UUID,
    var stackSize: Int = 1,
    var liveMobs: Int = 0,
    var enabled: Boolean = true,
) {

    /** `null` quando o enum foi renomeado e o registro ficou órfão. */
    fun type(): SpawnerType? = SpawnerType.find(typeId)
}
```

- [ ] **Step 2: PlacedSpawnerRepository**

`.../spawners/spawner/PlacedSpawnerRepository.kt`:

```kotlin
package one_block.system.spawners.spawner

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.UUID

class PlacedSpawnerRepository {

    private val collection: MongoCollection<PlacedSpawner> = Mongo.getCollection("spawners")

    /** Usado por [update]: insere o documento se ele ainda não existir. */
    private val upsertOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlacedSpawner> = collection.find().toList()

    /** Grava um spawner específico, criando o documento se necessário. */
    fun update(placed: PlacedSpawner) {
        collection.replaceOne(Filters.eq("uuid", placed.uuid), placed, upsertOptions)
    }

    /**
     * Flush em lote. **Sem** upsert de propósito: o snapshot é tirado na main
     * thread e despachado para o `ScheduledExecutor`, então pode chegar depois de
     * um `unregister` (jogador quebrou o bloco) já ter apagado o documento. Com
     * upsert, esse flush atrasado ressuscitaria um spawner fantasma.
     */
    fun updateAll(list: List<PlacedSpawner>) {
        if (list.isEmpty()) return

        val replaceOptions = ReplaceOptions().upsert(false)
        val models = list.map { ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions) }
        collection.bulkWrite(models)
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }
}
```

- [ ] **Step 3: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): modelo e repositório do spawner colocado"
```

---

### Task 5: Chaves de PDC e item do spawner

**Files:**
- Create: `.../spawners/spawner/SpawnerKeys.kt`
- Create: `.../spawners/spawner/SpawnerItems.kt`

**Interfaces:**
- Consumes: `SpawnerType` (Task 3)
- Produces:
  - `SpawnerKeys.TYPE_ID: NamespacedKey`, `SpawnerKeys.OWNER_SPAWNER: NamespacedKey`
  - `SpawnerItems.build(type: SpawnerType, amount: Int = 1): ItemStack`
  - `SpawnerItems.typeOf(item: ItemStack?): SpawnerType?`

- [ ] **Step 1: SpawnerKeys**

`.../spawners/spawner/SpawnerKeys.kt`:

```kotlin
package one_block.system.spawners.spawner

import org.bukkit.NamespacedKey

/** Chaves de PDC do módulo, em itens e entidades. */
object SpawnerKeys {

    /** No item: qual [SpawnerType] ele é. É o único estado que o item carrega. */
    val TYPE_ID = NamespacedKey("oneblock", "spawner_type_id")

    /**
     * Na entidade e no display: uuid do [PlacedSpawner] dono. É por esta marca
     * que a reconciliação encontra a entidade e que o listener de morte sabe que
     * a morte é nossa.
     */
    val OWNER_SPAWNER = NamespacedKey("oneblock", "spawner_owner")
}
```

- [ ] **Step 2: SpawnerItems**

`.../spawners/spawner/SpawnerItems.kt`:

```kotlin
package one_block.system.spawners.spawner

import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/**
 * Item que representa o spawner na mão.
 *
 * Carrega **só** o id do tipo no PDC — nenhum estado do bloco. É isso que faz
 * dois spawners do mesmo mob empilharem no inventário, que por sua vez é o que
 * dá sentido a "empilhar ao colocar".
 */
object SpawnerItems {

    fun build(type: SpawnerType, amount: Int = 1): ItemStack = itemStack(Material.SPAWNER) {
        this.amount = amount.coerceAtLeast(1)
        meta<ItemMeta> {
            name(type.displayName)
            setLore {
                +"<dark_gray>▸ <white>Tier: <aqua>${type.tier.prettyName}"
                +"<dark_gray>▸ <white>Ciclo: <aqua>${type.spawnIntervalTicks / 20}s"
                +"<dark_gray>▸ <white>Limite de mobs: <aqua>${type.maxLiveMobs}"
                +""
                +"<gray>Coloque na ilha e empilhe clicando"
                +"<gray>com outro spawner do mesmo mob."
            }
            nbt { string(SpawnerKeys.TYPE_ID, type.name) }
        }
    }

    /** Resolve o tipo de um item de spawner. `null` se não for um. */
    fun typeOf(item: ItemStack?): SpawnerType? {
        if (item == null || item.type != Material.SPAWNER) return null
        val meta = item.itemMeta ?: return null
        val id = meta.persistentDataContainer.get(SpawnerKeys.TYPE_ID, PersistentDataType.STRING) ?: return null
        return SpawnerType.find(id)
    }
}
```

- [ ] **Step 3: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`. Se o DSL de `nbt {}` ou `meta<ItemMeta> {}` reclamar, confira a assinatura em `lib/item-builder/ItemStackExt.kt` — o `VacuumHopper` é o exemplo em uso.

- [ ] **Step 4: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): item do spawner e chaves de PDC"
```

---

### Task 6: Entidade stackada e display

**Files:**
- Create: `.../spawners/entity/SpawnerEntity.kt`
- Create: `.../spawners/entity/SpawnerDisplay.kt`

**Interfaces:**
- Consumes: `PlacedSpawner` (Task 4), `SpawnerKeys` (Task 5), `SpawnerType` (Task 3), `Mini` (`lib:formatter`)
- Produces:
  - `SpawnerEntity.find(placed: PlacedSpawner): LivingEntity?`
  - `SpawnerEntity.reconcile(placed: PlacedSpawner, type: SpawnerType)`
  - `SpawnerEntity.remove(placed: PlacedSpawner)`
  - `SpawnerEntity.ownerOf(entity: Entity): UUID?`
  - `SpawnerDisplay.reconcile(placed: PlacedSpawner, type: SpawnerType)`
  - `SpawnerDisplay.remove(placed: PlacedSpawner)`

- [ ] **Step 1: SpawnerEntity**

`.../spawners/entity/SpawnerEntity.kt`:

```kotlin
package one_block.system.spawners.entity

import one_block.libs.formatter.Mini
import one_block.libs.instance.Instance
import one_block.system.spawners.spawner.PlacedSpawner
import one_block.system.spawners.spawner.SpawnerKeys
import one_block.system.spawners.spawner.SpawnerType
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * Entidade que representa o stack de mobs de um spawner.
 *
 * A entidade é **descartável**: o número real de mobs vive em
 * [PlacedSpawner.liveMobs], e esta classe só garante que o mundo mostre esse
 * número. Nada aqui é fonte de verdade — por isso não guardamos o uuid da
 * entidade em lugar nenhum, e sim a encontramos pela marca de PDC.
 */
object SpawnerEntity {

    /** Raio de busca da entidade em volta do bloco. */
    private const val SEARCH_RADIUS = 2.0

    /** Ponto onde a entidade fica: centro do bloco de cima. */
    private fun anchorOf(placed: PlacedSpawner): Location? =
        placed.location.toBukkit()?.add(0.5, 1.0, 0.5)

    fun ownerOf(entity: Entity): UUID? {
        val raw = entity.persistentDataContainer.get(SpawnerKeys.OWNER_SPAWNER, PersistentDataType.STRING)
            ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    fun find(placed: PlacedSpawner): LivingEntity? {
        val anchor = anchorOf(placed) ?: return null
        val world = anchor.world ?: return null
        if (!world.isChunkLoaded(anchor.blockX shr 4, anchor.blockZ shr 4)) return null

        return world.getNearbyEntities(anchor, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
            .filterIsInstance<LivingEntity>()
            .firstOrNull { ownerOf(it) == placed.uuid }
    }

    /**
     * Garante o invariante: com o chunk carregado e `liveMobs > 0`, existe
     * exatamente uma entidade viva marcada com o uuid deste spawner, mostrando o
     * número atual.
     */
    fun reconcile(placed: PlacedSpawner, type: SpawnerType) {
        val existing = find(placed)

        if (placed.liveMobs <= 0) {
            existing?.remove()
            return
        }

        if (existing != null) {
            applyName(existing, placed, type)
            return
        }

        val anchor = anchorOf(placed) ?: return
        val world = anchor.world ?: return

        // Resolvido antes da chamada: `entityClass` é nulo para tipos sem entidade
        // viva, e um `?: return` no meio da lista de argumentos do `spawn` piora a
        // leitura sem ganhar nada.
        val entityClass = type.entityType.entityClass
            ?.takeIf { LivingEntity::class.java.isAssignableFrom(it) }
            ?.asSubclass(LivingEntity::class.java)
            ?: run {
                Instance.logger.warning(
                    "[spawners] ${type.name} aponta para um EntityType que não é LivingEntity."
                )
                return
            }

        world.spawn(anchor, entityClass) { entity ->
            entity.setAI(false)
            entity.setSilent(true)
            entity.setGravity(false)
            // Descartável de propósito: não é salva com o chunk. O dado real está
            // no PlacedSpawner e a entidade é recriada quando o chunk volta.
            entity.isPersistent = false
            entity.setCanPickupItems(false)
            entity.persistentDataContainer.set(
                SpawnerKeys.OWNER_SPAWNER,
                PersistentDataType.STRING,
                placed.uuid.toString(),
            )
            applyName(entity, placed, type)
        }
    }

    fun remove(placed: PlacedSpawner) {
        find(placed)?.remove()
    }

    private fun applyName(entity: LivingEntity, placed: PlacedSpawner, type: SpawnerType) {
        entity.customName(Mini.deserialize("${type.displayName} <white>x${placed.liveMobs}"))
        entity.isCustomNameVisible = true
    }
}
```

- [ ] **Step 2: SpawnerDisplay**

`.../spawners/entity/SpawnerDisplay.kt`:

```kotlin
package one_block.system.spawners.entity

import one_block.libs.formatter.Mini
import one_block.system.spawners.spawner.PlacedSpawner
import one_block.system.spawners.spawner.SpawnerKeys
import one_block.system.spawners.spawner.SpawnerType
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/** Placa flutuante acima do bloco, com o mob, o stack e os mobs vivos. */
object SpawnerDisplay {

    private const val SEARCH_RADIUS = 1.5

    private fun anchorOf(placed: PlacedSpawner): Location? =
        placed.location.toBukkit()?.add(0.5, 1.8, 0.5)

    private fun ownerOf(display: TextDisplay): UUID? {
        val raw = display.persistentDataContainer.get(SpawnerKeys.OWNER_SPAWNER, PersistentDataType.STRING)
            ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    private fun find(placed: PlacedSpawner): TextDisplay? {
        val anchor = anchorOf(placed) ?: return null
        val world = anchor.world ?: return null
        if (!world.isChunkLoaded(anchor.blockX shr 4, anchor.blockZ shr 4)) return null

        return world.getNearbyEntities(anchor, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
            .filterIsInstance<TextDisplay>()
            .firstOrNull { ownerOf(it) == placed.uuid }
    }

    fun reconcile(placed: PlacedSpawner, type: SpawnerType) {
        val existing = find(placed)
        if (existing != null) {
            existing.text(textOf(placed, type))
            return
        }

        val anchor = anchorOf(placed) ?: return
        val world = anchor.world ?: return

        world.spawn(anchor, TextDisplay::class.java) { display ->
            display.isPersistent = false
            display.billboard = Display.Billboard.CENTER
            display.persistentDataContainer.set(
                SpawnerKeys.OWNER_SPAWNER,
                PersistentDataType.STRING,
                placed.uuid.toString(),
            )
            display.text(textOf(placed, type))
        }
    }

    fun remove(placed: PlacedSpawner) {
        find(placed)?.remove()
    }

    private fun textOf(placed: PlacedSpawner, type: SpawnerType) = Mini.deserialize(
        if (placed.enabled) {
            "${type.displayName} <dark_gray>x<white>${placed.stackSize}\n" +
                "<gray>Vivos: <aqua>${placed.liveMobs}<dark_gray>/<gray>${type.maxLiveMobs}"
        } else {
            "${type.displayName} <dark_gray>x<white>${placed.stackSize}\n<red>■ Desligado"
        }
    )
}
```

- [ ] **Step 3: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`. Se `world.spawn(...)` reclamar de tipo, confira a assinatura exata contra o jar do paper-api antes de improvisar — a API muda entre versões (ver CLAUDE.md).

- [ ] **Step 4: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): entidade stackada e display do bloco"
```

---

### Task 7: PlacedSpawnerManager — estado, flush e tick

**Files:**
- Create: `.../spawners/spawner/PlacedSpawnerManager.kt`

**Interfaces:**
- Consumes: `PlacedSpawner`, `PlacedSpawnerRepository` (Task 4), `SpawnRules`, `PlacementRules` (Task 2), `SpawnerEntity`, `SpawnerDisplay` (Task 6)
- Produces:
  - `PlacedSpawnerManager.onInit()`, `onDisable()`
  - `get(location: SimpleLocation): PlacedSpawner?`, `get(block: Block): PlacedSpawner?`, `get(uuid: UUID): PlacedSpawner?`
  - `keyOf(block: Block): SimpleLocation`, `all(): List<PlacedSpawner>`
  - `hasNeighborWithin(location: SimpleLocation): Boolean`
  - `register(placed: PlacedSpawner)`, `unregister(location: SimpleLocation): PlacedSpawner?`
  - `markDirty(placed: PlacedSpawner)`, `saveNow(placed: PlacedSpawner)`, `isRegistered(placed: PlacedSpawner): Boolean`

Este arquivo concentra as quatro armadilhas que o spec lista como preço do módulo
independente. Cada uma está comentada no ponto onde importa — não remova esses
comentários ao editar.

- [ ] **Step 1: Criar o manager**

`.../spawners/spawner/PlacedSpawnerManager.kt`:

```kotlin
package one_block.system.spawners.spawner

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.libs.instance.ScheduledExecutor
import one_block.system.spawners.entity.SpawnerDisplay
import one_block.system.spawners.entity.SpawnerEntity
import one_block.system.spawners.rules.PlacementRules
import one_block.system.spawners.rules.SpawnRules
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Fonte de verdade em runtime dos spawners colocados. Carrega tudo do Mongo no
 * boot, serve as consultas pelos mapas em memória e devolve ao banco em lote.
 */
object PlacedSpawnerManager {

    /** Intervalo do flush em lote das entradas sujas. */
    const val SAVE_INTERVAL_SECONDS = 10L

    private const val SAVE_INTERVAL_TICKS = SAVE_INTERVAL_SECONDS * 20L

    /** Atraso do boot, para o mundo já estar carregado quando os mapas enchem. */
    private const val INIT_DELAY_TICKS = 40L

    // Mapas concorrentes por segurança: o acesso normal é todo da main thread, mas
    // consultas vindas de outras threads não podem corromper a estrutura.
    private val byLocation = ConcurrentHashMap<SimpleLocation, PlacedSpawner>()
    private val byUuid = ConcurrentHashMap<UUID, PlacedSpawner>()
    private val dirtyIds = mutableSetOf<UUID>()

    /** Uuids cujo bloco físico já foi conferido pelo menos uma vez. */
    private val validated = mutableSetOf<UUID>()

    /** Uuids já reportados com [PlacedSpawner.typeId] sem [SpawnerType]. */
    private val warnedUnknownType = mutableSetOf<UUID>()

    /**
     * Ticks decorridos desde o último ciclo de spawn, por spawner. **Não** é
     * persistido: um restart reinicia o ciclo, custando no máximo um intervalo.
     * Persistir um timestamp abriria a porta para spawn retroativo ao carregar o
     * chunk — exatamente o acúmulo offline que este design não quer.
     */
    private val ticksSinceSpawn = ConcurrentHashMap<UUID, Long>()

    private val repository by lazy { PlacedSpawnerRepository() }

    fun onInit() {
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            repository.toList().forEach { index(it) }
            Instance.logger.info("[spawners] ${byUuid.size} spawner(s) carregado(s).")

            Bukkit.getScheduler().runTaskTimer(Instance, Runnable { processTick() }, 1L, 1L)
            // O flush vai no scheduler do Bukkit (e não no ScheduledExecutor)
            // porque o snapshot precisa ser tirado na main thread.
            Bukkit.getScheduler().runTaskTimer(
                Instance,
                Runnable { flushDirty() },
                SAVE_INTERVAL_TICKS,
                SAVE_INTERVAL_TICKS,
            )
        }, INIT_DELAY_TICKS)
    }

    /** Precisa ser chamado do `OneBlockPlugin.onDisable` — nada faz isso sozinho. */
    fun onDisable() {
        // No shutdown já estamos na main thread, então o snapshot é seguro; a
        // gravação vai síncrona porque o ScheduledExecutor pode já estar
        // encerrando e engoliria a última janela de flush.
        writeAll(takeDirtySnapshot())
    }

    fun keyOf(block: Block): SimpleLocation = SimpleLocation.fromBukkit(block.location)

    fun get(location: SimpleLocation): PlacedSpawner? = byLocation[location]

    fun get(block: Block): PlacedSpawner? = byLocation[keyOf(block)]

    fun get(uuid: UUID): PlacedSpawner? = byUuid[uuid]

    fun all(): List<PlacedSpawner> = byUuid.values.toList()

    /**
     * `true` se já existe spawner perto demais de [location]. Varredura linear
     * sobre o mapa: a ordem de grandeza é dezenas de spawners, não paga um índice
     * espacial.
     */
    fun hasNeighborWithin(location: SimpleLocation): Boolean =
        byUuid.values.any { other ->
            other.location.world == location.world &&
                PlacementRules.isTooClose(
                    location.x, location.y, location.z,
                    other.location.x, other.location.y, other.location.z,
                )
        }

    fun register(placed: PlacedSpawner) {
        index(placed)
        saveNow(placed)
    }

    fun unregister(location: SimpleLocation): PlacedSpawner? {
        val removed = byLocation.remove(location) ?: return null
        byUuid.remove(removed.uuid)
        validated.remove(removed.uuid)
        warnedUnknownType.remove(removed.uuid)
        ticksSinceSpawn.remove(removed.uuid)
        synchronized(dirtyIds) { dirtyIds.remove(removed.uuid) }

        SpawnerEntity.remove(removed)
        SpawnerDisplay.remove(removed)
        repository.remove(removed.uuid)
        return removed
    }

    /** `true` se este exato objeto ainda é o registro vivo daquele uuid. */
    fun isRegistered(placed: PlacedSpawner): Boolean = byUuid[placed.uuid] === placed

    /**
     * Marca para o flush em lote. Use no caminho quente (tick, morte).
     *
     * Objeto obsoleto (spawner quebrado com a GUI aberta) é ignorado: o
     * repositório grava com `upsert`, então marcar um uuid já apagado
     * ressuscitaria o documento no próximo flush.
     */
    fun markDirty(placed: PlacedSpawner) {
        if (!isRegistered(placed)) return
        synchronized(dirtyIds) { dirtyIds.add(placed.uuid) }
    }

    /** Persiste imediatamente. Use fora do caminho quente. */
    fun saveNow(placed: PlacedSpawner) {
        if (!isRegistered(placed)) return
        synchronized(dirtyIds) { dirtyIds.remove(placed.uuid) }
        repository.update(placed)
    }

    private fun index(placed: PlacedSpawner) {
        byLocation[placed.location] = placed
        byUuid[placed.uuid] = placed
    }

    /**
     * Roda na main thread: lê os sujos, copia e limpa as marcações num bloco só.
     * O I/O sai daqui com as **cópias** — entregar o objeto vivo ao driver do
     * Mongo faria o encoder ler campos que a main thread está alterando.
     */
    private fun takeDirtySnapshot(): List<PlacedSpawner> {
        val ids = synchronized(dirtyIds) {
            if (dirtyIds.isEmpty()) return emptyList()
            val copy = dirtyIds.toList()
            dirtyIds.clear()
            copy
        }

        return ids.mapNotNull { byUuid[it] }.map { it.copy() }
    }

    private fun writeAll(spawners: List<PlacedSpawner>) {
        if (spawners.isEmpty()) return
        runCatching { repository.updateAll(spawners) }
            .onFailure { Instance.logger.warning("[spawners] flush falhou: ${it.message}") }
    }

    private fun flushDirty() {
        val snapshot = takeDirtySnapshot()
        if (snapshot.isEmpty()) return

        // Snapshot na main thread, I/O fora dela: o Mongo não pode segurar o tick.
        ScheduledExecutor.execute { writeAll(snapshot) }
    }

    private fun processTick() {
        byUuid.values.toList().forEach { placed ->
            val type = placed.type() ?: run {
                warnUnknownType(placed)
                return@forEach
            }
            if (!isChunkLoaded(placed)) return@forEach
            if (!isStillValid(placed)) return@forEach

            if (!placed.enabled) {
                SpawnerEntity.remove(placed)
                SpawnerDisplay.reconcile(placed, type)
                return@forEach
            }

            val elapsed = (ticksSinceSpawn[placed.uuid] ?: 0L) + 1L
            if (elapsed >= type.spawnIntervalTicks) {
                ticksSinceSpawn[placed.uuid] = 0L
                if (SpawnRules.shouldSpawn(placed.enabled, placed.liveMobs, type.maxLiveMobs)) {
                    placed.liveMobs = SpawnRules.nextLiveMobs(placed.liveMobs, placed.stackSize, type.maxLiveMobs)
                    markDirty(placed)
                }
            } else {
                ticksSinceSpawn[placed.uuid] = elapsed
            }

            runCatching {
                SpawnerEntity.reconcile(placed, type)
                SpawnerDisplay.reconcile(placed, type)
            }.onFailure {
                Instance.logger.warning("[spawners] reconciliação de ${placed.uuid} falhou: ${it.message}")
            }
        }
    }

    /**
     * Registro apontando para um [SpawnerType] que não existe mais (enum
     * renomeado). O spawner fica inerte, mas **não** é apagado — o dado do
     * jogador não some por causa de um rename. Loga uma vez por uuid.
     */
    private fun warnUnknownType(placed: PlacedSpawner) {
        if (!warnedUnknownType.add(placed.uuid)) return
        Instance.logger.warning(
            "[spawners] spawner ${placed.uuid} em ${placed.location} tem typeId " +
                "'${placed.typeId}' sem SpawnerType correspondente."
        )
    }

    private fun isChunkLoaded(placed: PlacedSpawner): Boolean {
        val bukkit: Location = placed.location.toBukkit() ?: return false
        val world = bukkit.world ?: return false
        return world.isChunkLoaded(bukkit.blockX shr 4, bukkit.blockZ shr 4)
    }

    /**
     * Validação preguiçosa: no primeiro tick com o chunk carregado, confere se o
     * bloco físico ainda existe. WorldEdit, rollback ou backup deixariam um
     * registro fantasma spawnando mob para sempre num lugar vazio. Validar tudo
     * no boot forçaria carregar todos os chunks — caro e inútil.
     */
    private fun isStillValid(placed: PlacedSpawner): Boolean {
        if (placed.uuid in validated) return true

        val bukkit = placed.location.toBukkit() ?: return false
        if (bukkit.block.type != Material.SPAWNER) {
            Instance.logger.warning(
                "[spawners] spawner sumiu em ${placed.location}; registro removido."
            )
            unregister(placed.location)
            return false
        }

        validated.add(placed.uuid)
        return true
    }
}
```

- [ ] **Step 2: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): manager com tick, reconciliação e flush"
```

---

### Task 8: SpawnerBlockListener — colocar, empilhar, quebrar

**Files:**
- Create: `.../spawners/listener/SpawnerBlockListener.kt`

**Interfaces:**
- Consumes: `PlacedSpawnerManager` (Task 7), `SpawnerItems` (Task 5), `StackRules` (Task 2)
- Produces: `SpawnerBlockListener(viewFrame: ViewFrame) : Listener`

**Nota de ordem:** o clique com a mão vazia deve abrir a `SpawnerView`, criada na
Task 10. Até lá, `openMenu` envia uma mensagem provisória para o listener compilar
isolado; a Task 10 troca o corpo desse método. É a única dependência para frente
do plano.

- [ ] **Step 1: Criar o listener**

`.../spawners/listener/SpawnerBlockListener.kt`:

```kotlin
package one_block.system.spawners.listener

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.system.spawners.rules.StackRules
import one_block.system.spawners.spawner.PlacedSpawner
import one_block.system.spawners.spawner.PlacedSpawnerManager
import one_block.system.spawners.spawner.SpawnerItems
import one_block.system.spawners.spawner.SpawnerType
import org.bukkit.block.Block
import org.bukkit.block.CreatureSpawner
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

class SpawnerBlockListener(private val viewFrame: ViewFrame) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val type = SpawnerItems.typeOf(event.itemInHand) ?: return
        val player = event.player
        val block = event.blockPlaced

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode colocar isso aqui.")
            return
        }

        val location = PlacedSpawnerManager.keyOf(block)
        if (PlacedSpawnerManager.hasNeighborWithin(location)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Muito perto de outro spawner. <gray>(mínimo de 5 blocos)")
            return
        }

        val placed = PlacedSpawner(
            uuid = UUID.randomUUID(),
            location = location,
            typeId = type.name,
            ownerUuid = player.uniqueId,
        )

        applySpawnerState(block, type)
        PlacedSpawnerManager.register(placed)
        player.sendRichMessage("<green>Spawner colocado. <gray>Clique com outro na mão para empilhar.")
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        val placed = PlacedSpawnerManager.get(block) ?: return
        val type = placed.type() ?: return
        val player = event.player

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode quebrar isso.")
            return
        }

        // Sem isto o Bukkit dropa também o SPAWNER vanilla e duplica material.
        event.isDropItems = false
        val stack = placed.stackSize
        PlacedSpawnerManager.unregister(placed.location)

        val dropAt = block.location.clone().add(0.5, 0.5, 0.5)
        block.world.dropItemNaturally(dropAt, SpawnerItems.build(type, stack))
        player.sendRichMessage("<green>Spawner recolhido. <gray>(${stack}x)")
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        // Shift+clique continua servindo para colocar bloco na face.
        if (player.isSneaking) return

        val block = event.clickedBlock ?: return
        val placed = PlacedSpawnerManager.get(block) ?: return
        val type = placed.type() ?: return

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não tem acesso a este spawner.")
            return
        }

        event.isCancelled = true

        val inHand = player.inventory.itemInMainHand
        val handType = SpawnerItems.typeOf(inHand)

        if (handType == null) {
            openMenu(player, placed)
            return
        }

        if (handType != type) {
            player.sendRichMessage("<red>Esse spawner é de outro mob.")
            return
        }

        stackFromHand(player, placed, inHand.amount)
    }

    private fun stackFromHand(player: Player, placed: PlacedSpawner, offered: Int) {
        val result = StackRules.add(placed.stackSize, offered)
        if (result.consumed == 0) {
            player.sendRichMessage("<red>Este spawner já está no limite de <white>${StackRules.MAX_STACK}<red>.")
            return
        }

        placed.stackSize = result.newStack
        // amount = 0 não limpa o slot de forma confiável; zerou, tira o item.
        if (result.leftover <= 0) {
            player.inventory.setItemInMainHand(null)
        } else {
            player.inventory.itemInMainHand.amount = result.leftover
        }
        PlacedSpawnerManager.saveNow(placed)

        player.sendRichMessage(
            "<green>+${result.consumed} spawner(s). <gray>Total: <white>${placed.stackSize}<gray>/${StackRules.MAX_STACK}"
        )
    }

    /** Substituído na Task 10 pela abertura da `SpawnerView`. */
    private fun openMenu(player: Player, placed: PlacedSpawner) {
        player.sendRichMessage("<gray>Spawner: <white>${placed.stackSize}x<gray>, vivos: <white>${placed.liveMobs}")
    }

    /**
     * Deixa o bloco com a aparência certa e o spawn nativo desligado:
     * `requiredPlayerRange = 0` faz o vanilla nunca spawnar, e todo spawn passa a
     * vir do nosso tick.
     */
    private fun applySpawnerState(block: Block, type: SpawnerType) {
        val state = block.state as? CreatureSpawner ?: return
        state.spawnedType = type.entityType
        state.requiredPlayerRange = 0
        state.update(true, false)
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { PlacedSpawnerManager.get(it) != null }) event.isCancelled = true
    }

    @EventHandler
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { PlacedSpawnerManager.get(it) != null }) event.isCancelled = true
    }

    /**
     * Tira os spawners registrados da lista da explosão. Deixar explodir sumiria
     * com o bloco físico e manteria a entrada no Mongo — registro fantasma
     * spawnando mob num lugar vazio para sempre.
     */
    private fun protect(blocks: MutableList<Block>) {
        blocks.removeIf { PlacedSpawnerManager.get(it) != null }
    }

    private fun canManage(island: Island, player: Player): Boolean =
        island.ownerUuid == player.uniqueId || island.isMember(player.name)
}
```

- [ ] **Step 2: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): listener de colocar, empilhar e quebrar"
```

---

### Task 9: SpawnerDeathListener — dano, morte e loot

**Files:**
- Create: `.../spawners/listener/SpawnerDeathListener.kt`

**Interfaces:**
- Consumes: `SpawnerEntity.ownerOf` (Task 6), `PlacedSpawnerManager` (Task 7), `SpawnRules` (Task 2), `LootRoll` (Task 3)
- Produces: `SpawnerDeathListener : Listener`

Este é o ponto onde o dupe é fechado, e sem teste automatizado ele depende
inteiramente dos passos 8 e 9 do roteiro de verificação. Duas regras que precisam
sair juntas:

1. Todo dano que **não** venha de golpe de jogador é cancelado. É isso que dá
   "imune a ambiente" sem `setInvulnerable(true)`, que bloquearia o jogador junto.
2. Só o caminho de morte por jogador decrementa `liveMobs` e gera loot. Qualquer
   outra remoção — `/kill`, plugin, limpeza — não gera nada, e a entidade volta no
   tick seguinte com o número correto.

- [ ] **Step 1: Criar o listener**

`.../spawners/listener/SpawnerDeathListener.kt`:

```kotlin
package one_block.system.spawners.listener

import one_block.system.spawners.entity.SpawnerEntity
import one_block.system.spawners.rules.LootRoll
import one_block.system.spawners.rules.SpawnRules
import one_block.system.spawners.spawner.PlacedSpawnerManager
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetEvent

class SpawnerDeathListener : Listener {

    /**
     * Imunidade a ambiente. Não usamos `setInvulnerable(true)` porque essa flag
     * bloquearia também o golpe do jogador, tornando o mob imortal e o loot
     * inalcançável. Aqui, só o dano vindo de jogador sobrevive.
     */
    @EventHandler(ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (SpawnerEntity.ownerOf(entity) == null) return

        val byPlayer = event is EntityDamageByEntityEvent && event.damager is Player
        if (!byPlayer) event.isCancelled = true
    }

    /** Sem IA já basta, mas isto evita qualquer tentativa de mirar em jogador. */
    @EventHandler(ignoreCancelled = true)
    fun onTarget(event: EntityTargetEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (SpawnerEntity.ownerOf(entity) != null) event.isCancelled = true
    }

    @EventHandler
    fun onDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val ownerUuid = SpawnerEntity.ownerOf(entity) ?: return

        // O loot vanilla nunca vale: a tabela é nossa.
        event.drops.clear()
        event.droppedExp = 0

        // Morte que não é golpe de jogador não descasca nem gera loot. Fecha dupe
        // por /kill, plugin externo, limpeza de entidades e restauração de backup —
        // a entidade volta no tick seguinte com o número correto.
        val killer = entity.killer ?: return

        val placed = PlacedSpawnerManager.get(ownerUuid) ?: return
        val type = placed.type() ?: return

        placed.liveMobs = SpawnRules.afterKill(placed.liveMobs)
        PlacedSpawnerManager.markDirty(placed)

        val world = entity.location.world ?: return
        LootRoll.roll(type.loot).forEach { drop ->
            world.dropItemNaturally(entity.location, drop)
        }
        killer.giveExp(type.xpPerKill)
    }
}
```

- [ ] **Step 2: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): morte por jogador, loot próprio e imunidade a ambiente"
```

---

### Task 10: SpawnerView — a GUI do spawner

**Files:**
- Create: `.../spawners/view/SpawnerView.kt`
- Modify: `.../spawners/listener/SpawnerBlockListener.kt` (trocar o corpo de `openMenu`)

**Interfaces:**
- Consumes: `PlacedSpawner`, `PlacedSpawnerManager` (Task 7), `StackRules` (Task 2), `SpawnerItems` (Task 5)
- Produces: `SpawnerView : View`, aberta com `viewFrame.open(SpawnerView::class.java, player, placed)`

- [ ] **Step 1: Criar a view**

`.../spawners/view/SpawnerView.kt`:

```kotlin
package one_block.system.spawners.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.spawners.rules.StackRules
import one_block.system.spawners.spawner.PlacedSpawner
import one_block.system.spawners.spawner.PlacedSpawnerManager
import one_block.system.spawners.spawner.SpawnerItems
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SpawnerView : View() {

    private val spawnerState = initialState<PlacedSpawner>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Spawner")
        config.layout(
            "         ",
            " I T A B ",
            "   1 8 6 ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placed = spawnerState.get(render)

        render.layoutSlot('I').renderWith { statusItem(placed) }

        render.layoutSlot('T')
            .renderWith { toggleItem(placed) }
            .onClick { click ->
                if (!click.requireAlive(placed)) return@onClick
                placed.enabled = !placed.enabled
                PlacedSpawnerManager.saveNow(placed)
                click.update()
            }

        render.layoutSlot('1')
            .renderWith { withdrawItem(1) }
            .onClick { click -> withdraw(click, placed, 1) }

        render.layoutSlot('8')
            .renderWith { withdrawItem(8) }
            .onClick { click -> withdraw(click, placed, 8) }

        render.layoutSlot('6')
            .renderWith { withdrawItem(64) }
            .onClick { click -> withdraw(click, placed, 64) }

        render.layoutSlot('A')
            .renderWith { withdrawAllItem() }
            .onClick { click -> withdraw(click, placed, placed.stackSize) }

        render.layoutSlot('B').renderWith { infoItem() }
    }

    /**
     * Retirada parcial é o caminho normal, não erro: o que não couber no
     * inventário fica no bloco. Só remove o spawner quando o stack zera de fato —
     * um bloco não pode sumir com spawners dentro.
     */
    private fun withdraw(click: SlotClickContext, placed: PlacedSpawner, requested: Int) {
        if (!click.requireAlive(placed)) return

        val player = click.player
        val type = placed.type() ?: return
        val space = freeSpaceFor(player, type.name)

        val result = StackRules.withdraw(placed.stackSize, requested, space)
        if (result.withdrawn == 0) {
            player.sendRichMessage("<red>Sem espaço no inventário.")
            return
        }

        placed.stackSize = result.newStack
        player.inventory.addItem(SpawnerItems.build(type, result.withdrawn))

        if (result.newStack <= 0) {
            val block = placed.location.toBukkit()?.block
            PlacedSpawnerManager.unregister(placed.location)
            block?.type = Material.AIR
            player.sendRichMessage("<green>Spawner recolhido por inteiro.")
            click.closeForPlayer()
            return
        }

        PlacedSpawnerManager.saveNow(placed)
        if (result.withdrawn < requested) {
            player.sendRichMessage("<yellow>Retirado só <white>${result.withdrawn}<yellow>: inventário cheio.")
        }
        click.update()
    }

    /**
     * Quantos spawners cabem no inventário. Conta slot vazio como 64 e stack
     * parcial do mesmo tipo pelo que falta para encher.
     */
    private fun freeSpaceFor(player: Player, typeName: String): Int {
        var space = 0
        player.inventory.storageContents.forEach { item ->
            if (item == null || item.type.isAir) {
                space += 64
                return@forEach
            }
            val itemType = SpawnerItems.typeOf(item) ?: return@forEach
            if (itemType.name == typeName) space += (item.maxStackSize - item.amount).coerceAtLeast(0)
        }
        return space
    }

    private fun statusItem(placed: PlacedSpawner): ItemStack {
        val type = placed.type()
        return itemStack(Material.SPAWNER) {
            meta {
                name(type?.displayName ?: "<red>Spawner desconhecido")
                setLore {
                    +"<dark_gray>▸ <white>Empilhados: <aqua>${placed.stackSize}<gray>/${StackRules.MAX_STACK}"
                    +"<dark_gray>▸ <white>Mobs vivos: <aqua>${placed.liveMobs}<gray>/${type?.maxLiveMobs ?: 0}"
                    +"<dark_gray>▸ <white>Nascem por ciclo: <aqua>${placed.stackSize}"
                    +"<dark_gray>▸ <white>Ciclo: <aqua>${(type?.spawnIntervalTicks ?: 0L) / 20}s"
                    +""
                    +if (placed.enabled) "<green>▶ Ligado" else "<red>■ Desligado"
                }
            }
        }
    }

    private fun toggleItem(placed: PlacedSpawner) =
        itemStack(if (placed.enabled) Material.LIME_DYE else Material.GRAY_DYE) {
            meta {
                name(if (placed.enabled) "<green>Ligado" else "<red>Desligado")
                setLore {
                    +"<gray>Desligado, o spawner para de gerar"
                    +"<gray>mobs, mas <white>não perde<gray> os que já nasceram."
                    +""
                    +"<yellow>Clique para alternar."
                }
            }
        }

    private fun withdrawItem(amount: Int) = itemStack(Material.HOPPER) {
        meta {
            name("<yellow>Retirar ${amount}x")
            setLore {
                +"<gray>Devolve spawners para o seu inventário."
            }
        }
    }

    private fun withdrawAllItem() = itemStack(Material.CHEST) {
        meta {
            name("<gold>Retirar tudo")
            setLore {
                +"<gray>Retira o stack inteiro e remove o bloco."
                +"<gray>Se não couber tudo, retira o que couber."
            }
        }
    }

    private fun infoItem() = itemStack(Material.BOOK) {
        meta {
            name("<aqua>Como funciona")
            setLore {
                +"<gray>Cada ciclo gera <white>1 mob por spawner<gray> empilhado."
                +"<gray>Os mobs ficam juntos numa entidade só."
                +"<gray>Cada golpe letal mata <white>um<gray> deles."
                +""
                +"<gray>Empilhe clicando no bloco com outro"
                +"<gray>spawner do mesmo mob na mão."
            }
        }
    }

    /**
     * Bloqueia cliques numa GUI cujo spawner já foi quebrado. Sem isto, um clique
     * atrasado gravaria de volta um registro apagado.
     */
    private fun SlotClickContext.requireAlive(placed: PlacedSpawner): Boolean {
        if (PlacedSpawnerManager.isRegistered(placed)) return true
        player.sendRichMessage("<red>Este spawner não existe mais.")
        closeForPlayer()
        return false
    }
}
```

- [ ] **Step 2: Ligar a GUI ao listener**

Em `.../spawners/listener/SpawnerBlockListener.kt`, adicionar o import:

```kotlin
import one_block.system.spawners.view.SpawnerView
```

E substituir o `openMenu` provisório da Task 8 por:

```kotlin
    private fun openMenu(player: Player, placed: PlacedSpawner) {
        viewFrame.open(SpawnerView::class.java, player, placed)
    }
```

- [ ] **Step 3: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`. Se `viewFrame.open(...)` reclamar da assinatura, use `VacuumHopperView`/`HopperStorageView` como referência — o projeto já abre views com estado.

- [ ] **Step 4: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): GUI com info, retirada e liga/desliga"
```

---

### Task 11: Loja e comando

**Files:**
- Create: `.../spawners/view/SpawnerShopView.kt`
- Create: `.../spawners/command/SpawnerCommand.kt`

**Interfaces:**
- Consumes: `SpawnerType` (Task 3), `SpawnerItems` (Task 5), `Currencies` (system `currencies`)
- Produces: `SpawnerShopView : View`, `SpawnerCommand(viewFrame: ViewFrame) : BaseCommand`

- [ ] **Step 1: Criar a loja**

`.../spawners/view/SpawnerShopView.kt`:

```kotlin
package one_block.system.spawners.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.currencies.Currencies
import one_block.system.spawners.spawner.SpawnerItems
import one_block.system.spawners.spawner.SpawnerType
import org.bukkit.Material
import org.bukkit.entity.Player

class SpawnerShopView : View() {

    private companion object {
        const val CURRENCY = "coins"
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Loja de Spawners")
        config.size(if (SpawnerType.entries.size <= 27) 3 else 6)
    }

    override fun onFirstRender(render: RenderContext) {
        SpawnerType.entries.sortedBy { it.tier.ordinal }.forEach { type ->
            render.availableSlot()
                .renderWith { offerItem(type) }
                .onClick { click -> buy(click.player, type) }
        }
    }

    private fun buy(player: Player, type: SpawnerType) {
        if (!Currencies.has(player.uniqueId, CURRENCY, type.price)) {
            player.sendRichMessage("<red>Coins insuficientes. <gray>(custa ${type.price.toLong()})")
            return
        }

        // Confere o espaço antes de cobrar: debitar e não conseguir entregar seria
        // o pior resultado possível para o jogador.
        if (player.inventory.firstEmpty() == -1) {
            player.sendRichMessage("<red>Seu inventário está cheio.")
            return
        }

        Currencies.withdraw(player.uniqueId, CURRENCY, type.price)
        player.inventory.addItem(SpawnerItems.build(type))
        player.sendRichMessage("<green>Comprado: <white>${type.name}<green> por <white>${type.price.toLong()} coins<green>.")
    }

    private fun offerItem(type: SpawnerType) = itemStack(Material.SPAWNER) {
        meta {
            name(type.displayName)
            setLore {
                +"<dark_gray>▸ <white>Tier: <aqua>${type.tier.prettyName}"
                +"<dark_gray>▸ <white>Preço: <gold>${type.price.toLong()} coins"
                +"<dark_gray>▸ <white>Ciclo: <aqua>${type.spawnIntervalTicks / 20}s"
                +"<dark_gray>▸ <white>Limite de mobs: <aqua>${type.maxLiveMobs}"
                +""
                +"<yellow>Clique para comprar."
            }
        }
    }
}
```

- [ ] **Step 2: Criar o comando**

`.../spawners/command/SpawnerCommand.kt`:

```kotlin
package one_block.system.spawners.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.spawners.spawner.SpawnerItems
import one_block.system.spawners.spawner.SpawnerType
import one_block.system.spawners.view.SpawnerShopView
import org.bukkit.entity.Player

@CommandAlias("spawner|spawners")
class SpawnerCommand(private val viewFrame: ViewFrame) : BaseCommand() {

    @Subcommand("loja|shop")
    fun shop(sender: Player) {
        viewFrame.open(SpawnerShopView::class.java, sender)
    }

    @Subcommand("give")
    @CommandPermission("screen.dev")
    @CommandCompletion("@spawnertypes @players @range:1-64")
    fun give(
        sender: Player,
        id: String,
        @Optional target: OnlinePlayer?,
        @Default("1") @Optional amount: Int,
    ) {
        val type = SpawnerType.find(id.uppercase())
        if (type == null) {
            sender.sendRichMessage("<red>Spawner desconhecido: <white>$id")
            return
        }

        val receiver = target?.player ?: sender
        val amt = amount.coerceIn(1, 64)
        val overflow = receiver.inventory.addItem(SpawnerItems.build(type, amt))

        overflow.values.forEach { receiver.world.dropItemNaturally(receiver.location, it) }
        sender.sendRichMessage("<green>Entregue <white>${amt}x ${type.name}<green> para <white>${receiver.name}")
    }

    @Subcommand("list")
    @CommandPermission("screen.dev")
    fun list(sender: Player) {
        val ids = SpawnerType.entries.joinToString(", ") { it.name }
        sender.sendRichMessage("<yellow>Spawners (${SpawnerType.entries.size}): <white>$ids")
    }
}
```

- [ ] **Step 3: Verificar**

Run: `./gradlew :plugins:oneblock:system:spawners:localCompile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add plugins/oneblock/system/spawners/
git commit -m "feat(spawners): loja por coins e comando de admin"
```

---

### Task 12: Wiring e limpeza de órfãos

**Files:**
- Create: `.../spawners/SpawnersSystem.kt`
- Modify: `plugins/oneblock/plugin/build.gradle.kts` (só se a lista de systems não for automática — conferir)
- Modify: `plugins/oneblock/plugin/src/main/kotlin/io/github/badnotice/one_block/OneBlockPlugin.kt`

**Interfaces:**
- Consumes: tudo das tasks anteriores
- Produces: `SpawnersSystem.init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager)`

A limpeza de órfãos entra aqui porque depende do manager já ter carregado os
registros. Sem ela, um servidor que caiu entre remover o bloco e limpar as
entidades deixa no mundo um mob imune a dano de ambiente e sem dono — o pior modo
de falha do sistema, já que nada além de `/kill` o remove.

- [ ] **Step 1: Criar o wiring**

`.../spawners/SpawnersSystem.kt`:

```kotlin
package one_block.system.spawners

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.instance.Instance
import one_block.system.spawners.command.SpawnerCommand
import one_block.system.spawners.entity.SpawnerEntity
import one_block.system.spawners.listener.SpawnerBlockListener
import one_block.system.spawners.listener.SpawnerDeathListener
import one_block.system.spawners.spawner.PlacedSpawnerManager
import one_block.system.spawners.spawner.SpawnerType
import one_block.system.spawners.view.SpawnerShopView
import one_block.system.spawners.view.SpawnerView
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin

object SpawnersSystem {

    /** Espera o manager terminar de carregar (ver `INIT_DELAY_TICKS`) antes de varrer. */
    private const val CLEANUP_DELAY_TICKS = 80L

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        registerCompletions(commandManager)

        plugin.server.pluginManager.registerEvents(SpawnerBlockListener(viewFrame), plugin)
        plugin.server.pluginManager.registerEvents(SpawnerDeathListener(), plugin)

        viewFrame.with(SpawnerView(), SpawnerShopView())
        commandManager.registerCommand(SpawnerCommand(viewFrame))

        PlacedSpawnerManager.onInit()
        scheduleOrphanCleanup(plugin)
    }

    private fun registerCompletions(commandManager: PaperCommandManager) {
        commandManager.commandCompletions.registerCompletion("spawnertypes") {
            SpawnerType.entries.map { it.name }
        }
    }

    /**
     * Remove entidades e displays marcados por este módulo cujo spawner não existe
     * mais. Acontece quando o servidor cai entre apagar o registro e limpar as
     * entidades, ou quando alguém mexe no Mongo à mão.
     */
    private fun scheduleOrphanCleanup(plugin: JavaPlugin) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            var removed = 0
            Bukkit.getWorlds().forEach { world ->
                world.entities.forEach { entity: Entity ->
                    val owner = SpawnerEntity.ownerOf(entity) ?: return@forEach
                    if (PlacedSpawnerManager.get(owner) == null) {
                        entity.remove()
                        removed++
                    }
                }
            }
            if (removed > 0) {
                Instance.logger.info("[spawners] $removed entidade(s) órfã(s) removida(s).")
            }
        }, CLEANUP_DELAY_TICKS)
    }
}
```

- [ ] **Step 2: Conferir se o plugin já shada o módulo**

Run: `cat plugins/oneblock/plugin/build.gradle.kts`

Se as dependências de `:plugins:oneblock:system:*` vêm de um loop sobre os subprojetos, nada a fazer. Se são listadas uma a uma, acrescente a linha do módulo `spawners` no formato das vizinhas.

- [ ] **Step 3: Ligar no OneBlockPlugin**

Em `plugins/oneblock/plugin/src/main/kotlin/io/github/badnotice/one_block/OneBlockPlugin.kt`, adicionar os imports:

```kotlin
import one_block.system.spawners.SpawnersSystem
import one_block.system.spawners.spawner.PlacedSpawnerManager
```

Adicionar à lista de `setupSystems()`, junto das outras:

```kotlin
            { SpawnersSystem.init(this, viewFrame, paperCommandManager) },
```

E — **passo que nada faz sozinho** — adicionar ao `onDisable()`:

```kotlin
    override fun onDisable() {
        CoinFlipManager.onDisable()
        PlacedMinionManager.onDisable()
        PlacedBlockManager.onDisable()
        PlacedSpawnerManager.onDisable()
    }
```

Sem essa última linha, toda parada de servidor perde até 10 segundos de alterações.

- [ ] **Step 4: Build completo e deploy**

Run: `./gradlew :plugins:oneblock:plugin:localDeploy`
Expected: `BUILD SUCCESSFUL` e o jar copiado para `server/plugins/`. O servidor precisa estar **parado** — o Windows trava o jar em execução e a cópia falha.

- [ ] **Step 5: Commit**

```bash
git add plugins/oneblock/system/spawners/ plugins/oneblock/plugin/
git commit -m "feat(spawners): wiring do sistema e limpeza de entidades órfãs"
```

---

## Verificação em servidor

Não há testes automatizados: **este roteiro é a única verificação do sistema.**
Rode inteiro, na ordem, antes de considerar a feature pronta. Os passos 8 a 11 são
os que cobrem dupe e imunidade — falha ali é perda de economia, não bug cosmético.

1. `/spawner give ZOMBIE 5` → o item empilha no inventário com outro igual.
2. Colocar na ilha → vira spawner de zumbi, display aparece, mensagem de confirmação.
3. Colocar outro a 3 blocos → recusado com a mensagem de distância.
4. Colocar outro a 6 blocos → aceito.
5. Clicar no primeiro com 10 spawners na mão → stack vai a 11, a mão esvazia, o display atualiza.
6. Clicar com spawner de **outro mob** na mão → recusado, nada é consumido.
7. Esperar um ciclo → nasce "Zumbi x11", parado, sem atacar; esperar mais ciclos e conferir que o número sobe **na mesma entidade**, sem criar novas.
8. Bater até matar um → número cai de exatamente 1, o loot da tabela cai no chão, o XP entra.
9. `/kill @e[type=zombie]` → **nenhum** loot cai e o número não muda; a entidade volta no tick seguinte igual.
10. Empurrar o mob para lava / bater nele com fogo → não toma dano.
11. Deixar o mob perto de um jogador → não ataca nem mira.
12. Desligar na GUI → a entidade some e o número é preservado; religar → volta com o mesmo número.
13. Retirar 8 na GUI → 8 spawners no inventário, o stack cai.
14. Encher o inventário e clicar "retirar tudo" → retira o que couber, o bloco continua de pé, o aviso aparece.
15. Retirar tudo com inventário vazio → bloco some, item volta inteiro.
16. Quebrar o bloco com stack alto → devolve o stack inteiro num item, entidade e display somem.
17. Reiniciar o servidor com mobs vivos → ao voltar, a entidade é recriada com o número salvo.
18. Explodir um creeper ao lado → o bloco não some.
19. Empurrar com pistão → não move.
20. `/spawner loja` → comprar debita coins e entrega o item; comprar sem saldo é recusado.

## Pendências conhecidas

- **Sem testes automatizados.** A aritmética de `rules/` (teto de stack, sobra ao empilhar, corte no `maxLiveMobs`, faixa do loot) é o tipo de erro que passa despercebido em teste manual, porque o resultado errado ainda parece plausível em jogo. Se o sistema ganhar testes depois, é por `rules/` que eles começam — as funções lá já são puras e sem estado.
- **Ilha deletada** não limpa spawners. `IslandManager` não expõe exclusão hoje; se ela for implementada, precisa varrer e remover os `PlacedSpawner` da região.
