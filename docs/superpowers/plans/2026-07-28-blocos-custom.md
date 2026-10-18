# Blocos custom no Textura — plano de implementação

> **Para agentes:** execute tarefa por tarefa. Os passos usam checkbox (`- [ ]`).
> **Este repositório não usa testes automatizados e não faz commits pelo agente.** A verificação de cada tarefa é a compilação. O usuário commita.

**Objetivo:** permitir retexturar blocos vanilla (`DEEPSLATE_EMERALD_ORE` → Minério de Rubi) e criar blocos custom de verdade sobre as variantes de blockstate do `note_block`.

**Arquitetura:** o Textura gera os assets (blockstates, modelos, texturas, lang) e aloca as variantes; o `system/custom-items` do oneblock cuida do gameplay (colocar, quebrar, drop, som, partícula, guardas de integridade). A identidade de um bloco custom colocado vem da própria blockstate, não do Mongo.

**Stack:** Kotlin 2.3 / JDK 21, Paper 1.21.x, team.unnamed `creative` (montagem do pack), Bukkit `BlockData`.

**Spec:** [`docs/superpowers/specs/2026-07-28-blocos-custom-design.md`](../specs/2026-07-28-blocos-custom-design.md)

## Restrições globais

- Comentários e strings visíveis ao jogador em **português**.
- Nenhum teste automatizado. Nenhum `git commit` pelo agente.
- Erro de configuração **nunca** derruba o load: loga aviso e pula a entrada. Mesmo padrão de `ItemRegistry` e `PhaseManager`.
- Todo build local precisa de `local` no nome da task (ver `CLAUDE.md`). Use `./gradlew :plugins:textura:localJar` e `./gradlew :plugins:oneblock:plugin:localDeploy`.
- O Textura é um jar separado, **não** shaded no oneblock. `custom-items` o consome via `compileOnly(projects.plugins.textura)` e resolve em runtime.
- `system/*` nunca depende do módulo `plugins/oneblock/plugin`.
- Nenhum `!!`. Nulos tratados com `?:`, `?.let`, ou early return.

## Estrutura de arquivos

**Textura** (`plugins/textura/src/main/kotlin/one_block/textura/`)

| Arquivo | Responsabilidade |
|---|---|
| `model/BlockDefinition.kt` | Dados de uma entrada de bloco + enum do tipo. Sem lógica |
| `registry/BlockRegistry.kt` | Lê `configs/blocks/*.yml`, valida, guarda o mapa |
| `block/NoteBlockStates.kt` | Conversão índice ↔ string de blockstate. Sem estado |
| `block/BlockStateAllocator.kt` | Interface da alocação (fase 2 pluga outras bases aqui) |
| `block/NoteBlockAllocator.kt` | Alocação sobre note_block + persistência em `generated/block-allocations.yml` |
| `block/BlockModelWriter.kt` | Gera JSON de modelo (`cube_all` / `cube_bottom_top` / `cube`) |
| `api/BlockApi.kt` | Fachada pública para outros plugins |

Modificados: `util/ResourcePackBuilder.kt` (ganha `addBlocks` e `addLangOverrides`), `TexturaPlugin.kt` (wiring + reload).

**oneblock** (`plugins/oneblock/system/custom-items/src/main/kotlin/one_block/system/custom_items/block/`)

| Arquivo | Responsabilidade |
|---|---|
| `NoteBlockGuard.kt` | Os quatro cancelamentos que impedem o bloco de perder identidade |
| `BlockBreakEffects.kt` | Caminho compartilhado de quebra (drop + som + partícula) |
| `VanillaOverride.kt` | Dados de um override de bloco vanilla |
| `VanillaOverrideRegistry.kt` | Registro dos overrides + fórmula de fortuna |
| `VanillaOverrideListener.kt` | Intercepta quebra dos materiais registrados |

Modificados: `CustomBlock.kt`, `CustomBlockListener.kt`, `CustomBlockRegistry.kt`, `CustomItemSystem.kt`, `build.gradle.kts`.

---

## Tarefa 1: modelo e registry de blocos no Textura

**Arquivos:**
- Criar: `plugins/textura/src/main/kotlin/one_block/textura/model/BlockDefinition.kt`
- Criar: `plugins/textura/src/main/kotlin/one_block/textura/registry/BlockRegistry.kt`

**Interfaces:**
- Consome: `one_block.textura.util.PackAssets` (já existe).
- Produz: `BlockDefinition`, `BlockEntryType`, `BlockBase`, `BlockRegistry.load(File)`, `BlockRegistry.blocks: Map<String, BlockDefinition>`.

- [ ] **Passo 1: criar `BlockDefinition.kt`**

```kotlin
package one_block.textura.model

enum class BlockEntryType { OVERRIDE, CUSTOM }

/** Bloco vanilla que hospeda as variantes. Fase 1 só tem note_block. */
enum class BlockBase { NOTE_BLOCK }

/**
 * Uma entrada de `configs/blocks/*.yml`.
 *
 * `OVERRIDE` só troca a textura (e opcionalmente o nome) de um bloco vanilla —
 * nenhum código de servidor participa. `CUSTOM` cria um bloco novo sobre uma
 * variante de blockstate de [base].
 */
data class BlockDefinition(
    val id: String,
    val type: BlockEntryType,
    /** Material vanilla retexturado. Só em `OVERRIDE`. */
    val target: String? = null,
    /** Nome exibido, injetado no lang. */
    val name: String? = null,
    val base: BlockBase = BlockBase.NOTE_BLOCK,
    /** PNG única — gera `cube_all`. */
    val texture: String? = null,
    /** Faces nomeadas (`top`, `bottom`, `side`, `north`...). */
    val textures: Map<String, String> = emptyMap(),
    /** JSON de modelo próprio, copiado sem tradução. */
    val model: String? = null,
    /** Material base do item que representa o bloco no inventário. */
    val itemMaterial: String = DEFAULT_ITEM_MATERIAL,
) {
    /** Chave do lang que recebe [name]. */
    val translationKey: String
        get() = when (type) {
            BlockEntryType.OVERRIDE -> "block.minecraft.$target"
            BlockEntryType.CUSTOM -> "block.$NAMESPACE.$id"
        }

    /** Chave do componente `minecraft:item_model` do item deste bloco. */
    val itemModelKey: String get() = "$NAMESPACE:$id"

    /** Caminho do modelo de bloco dentro do pack. */
    val blockModelKey: String get() = "$NAMESPACE:block/$id"

    /** Caminho do modelo de item (o ícone 3D no inventário). */
    val itemModelPath: String get() = "$NAMESPACE:item/$id"

    companion object {
        const val NAMESPACE = "oneblock"
        const val DEFAULT_ITEM_MATERIAL = "paper"
    }
}
```

- [ ] **Passo 2: criar `BlockRegistry.kt`**

A leitura recursiva de `.yml` é a mesma de `ItemRegistry.collectYamlFiles` — repetida aqui de propósito, porque os dois registries evoluem separados e a abstração comum não pagaria o acoplamento.

```kotlin
package one_block.textura.registry

import one_block.textura.model.BlockBase
import one_block.textura.model.BlockDefinition
import one_block.textura.model.BlockEntryType
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Logger

class BlockRegistry(private val logger: Logger) {

    val blocks = mutableMapOf<String, BlockDefinition>()

    fun load(blocksFolder: File) {
        blocks.clear()

        if (!blocksFolder.exists()) {
            blocksFolder.mkdirs()
            return
        }

        collectYamlFiles(blocksFolder).forEach { loadYaml(it) }

        val custom = blocks.values.count { it.type == BlockEntryType.CUSTOM }
        val override = blocks.size - custom
        logger.info("Foram carregados $custom bloco(s) custom e $override override(s) de bloco vanilla.")
    }

    private fun collectYamlFiles(dir: File): List<File> {
        val result = mutableListOf<File>()
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                result.addAll(collectYamlFiles(file))
            } else if (file.name.endsWith(".yml")) {
                result.add(file)
            }
        }
        return result
    }

    private fun loadYaml(file: File) {
        val config = YamlConfiguration.loadConfiguration(file)
        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key) ?: continue

            val typeStr = section.getString("type", "CUSTOM")?.uppercase() ?: "CUSTOM"
            val type = runCatching { BlockEntryType.valueOf(typeStr) }.getOrNull()
            if (type == null) {
                logger.warning("Bloco '$key': tipo invalido '$typeStr'. Use OVERRIDE ou CUSTOM.")
                continue
            }

            val definition = when (type) {
                BlockEntryType.OVERRIDE -> parseOverride(key, section)
                BlockEntryType.CUSTOM -> parseCustom(key, section)
            } ?: continue

            if (blocks.containsKey(key)) {
                logger.warning("Bloco '$key' duplicado; a segunda definicao foi ignorada.")
                continue
            }
            blocks[key] = definition
        }
    }

    private fun parseOverride(
        key: String,
        section: org.bukkit.configuration.ConfigurationSection,
    ): BlockDefinition? {
        val target = section.getString("target")?.lowercase()
        if (target == null) {
            logger.warning("Bloco '$key' (OVERRIDE) nao declara 'target'.")
            return null
        }
        if (org.bukkit.Material.matchMaterial(target) == null) {
            logger.warning("Bloco '$key' (OVERRIDE): '$target' nao e um material valido.")
            return null
        }

        val texture = section.getString("texture")
        if (texture == null) {
            logger.warning("Bloco '$key' (OVERRIDE) nao declara 'texture'.")
            return null
        }

        return BlockDefinition(
            id = key,
            type = BlockEntryType.OVERRIDE,
            target = target,
            name = section.getString("name"),
            texture = texture,
        )
    }

    private fun parseCustom(
        key: String,
        section: org.bukkit.configuration.ConfigurationSection,
    ): BlockDefinition? {
        val baseStr = section.getString("base", "NOTE_BLOCK")?.uppercase() ?: "NOTE_BLOCK"
        val base = runCatching { BlockBase.valueOf(baseStr) }.getOrNull()
        if (base == null) {
            logger.warning("Bloco '$key': base invalida '$baseStr'. Na fase 1 so NOTE_BLOCK e suportada.")
            return null
        }

        val texture = section.getString("texture")
        val textures = section.getConfigurationSection("textures")
            ?.let { faces -> faces.getKeys(false).mapNotNull { face -> faces.getString(face)?.let { face to it } } }
            ?.toMap()
            .orEmpty()
        val model = section.getString("model")

        val fontes = listOfNotNull(
            texture?.let { "texture" },
            textures.takeIf { it.isNotEmpty() }?.let { "textures" },
            model?.let { "model" },
        )
        if (fontes.isEmpty()) {
            logger.warning("Bloco '$key' (CUSTOM) nao declara aparencia: use 'texture', 'textures' ou 'model'.")
            return null
        }
        // 'model' + 'textures' e a combinacao legitima: o JSON proprio referencia
        // as texturas declaradas. Qualquer outra sobreposicao e ambigua.
        val combinacaoValida = fontes == listOf("model") ||
            fontes == listOf("textures") ||
            fontes == listOf("texture") ||
            fontes == listOf("textures", "model")
        if (!combinacaoValida) {
            logger.warning("Bloco '$key' (CUSTOM): aparencia ambigua (${fontes.joinToString(" + ")}).")
            return null
        }

        val itemMaterial = section.getString("item_material", BlockDefinition.DEFAULT_ITEM_MATERIAL)
            ?.lowercase()
            ?: BlockDefinition.DEFAULT_ITEM_MATERIAL
        if (org.bukkit.Material.matchMaterial(itemMaterial) == null) {
            logger.warning("Bloco '$key' (CUSTOM): item_material '$itemMaterial' nao e um material valido.")
            return null
        }

        return BlockDefinition(
            id = key,
            type = BlockEntryType.CUSTOM,
            name = section.getString("name"),
            base = base,
            texture = texture,
            textures = textures,
            model = model,
            itemMaterial = itemMaterial,
        )
    }
}
```

- [ ] **Passo 3: compilar**

```bash
./gradlew :plugins:textura:localJar
```

Esperado: BUILD SUCCESSFUL. O registry ainda não é chamado por ninguém — isso é normal nesta tarefa.

---

## Tarefa 2: alocação de blockstate

**Arquivos:**
- Criar: `plugins/textura/src/main/kotlin/one_block/textura/block/NoteBlockStates.kt`
- Criar: `plugins/textura/src/main/kotlin/one_block/textura/block/BlockStateAllocator.kt`
- Criar: `plugins/textura/src/main/kotlin/one_block/textura/block/NoteBlockAllocator.kt`

**Interfaces:**
- Consome: `BlockDefinition` (Tarefa 1).
- Produz: `NoteBlockStates.CAPACITY: Int`, `NoteBlockStates.propertiesOf(index): String`, `NoteBlockStates.indexOf(properties): Int?`, `BlockStateAllocator` (interface), `NoteBlockAllocator(File, Logger)` com `sync(ids)`, `indexOf(id)`, `propertiesOf(id)`, `idByProperties(props)`, `allocations()`.

- [ ] **Passo 1: criar `NoteBlockStates.kt`**

```kotlin
package one_block.textura.block

/**
 * Conversão entre índice e blockstate de `note_block`.
 *
 * São 23 instrumentos × 25 notas × 2 estados de powered = 1150 combinações. O
 * índice 0 (`harp/0/false`) fica reservado para o note block vanilla, restando
 * 1149 slots alocáveis.
 *
 * A ordem é fixa e faz parte do contrato persistido: mudar a lista de
 * instrumentos ou a aritmética reatribui blocos já colocados no mundo.
 */
object NoteBlockStates {

    val INSTRUMENTS = listOf(
        "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar",
        "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit",
        "banjo", "pling", "zombie", "skeleton", "creeper", "dragon",
        "wither_skeleton", "piglin", "custom_head",
    )

    const val NOTES = 25
    const val POWERED_STATES = 2

    /** Total de combinações, incluindo a reservada. */
    val CAPACITY = INSTRUMENTS.size * NOTES * POWERED_STATES

    /** Primeiro índice alocável. O 0 é o note block vanilla. */
    const val FIRST_ALLOCATABLE = 1

    /** `instrument=harp,note=0,powered=false` — a chave do variant no blockstate JSON. */
    fun propertiesOf(index: Int): String {
        require(index in 0 until CAPACITY) { "indice de blockstate fora do intervalo: $index" }
        val powered = index % POWERED_STATES == 1
        val note = (index / POWERED_STATES) % NOTES
        val instrument = INSTRUMENTS[index / (POWERED_STATES * NOTES)]
        return "instrument=$instrument,note=$note,powered=$powered"
    }

    /** Inverso de [propertiesOf]. `null` quando a string não descreve um note_block válido. */
    fun indexOf(properties: String): Int? {
        val pares = properties.split(',')
            .mapNotNull { par ->
                val partes = par.split('=', limit = 2)
                if (partes.size == 2) partes[0].trim() to partes[1].trim() else null
            }
            .toMap()

        val instrument = INSTRUMENTS.indexOf(pares["instrument"] ?: return null)
        if (instrument < 0) return null
        val note = pares["note"]?.toIntOrNull()?.takeIf { it in 0 until NOTES } ?: return null
        val powered = when (pares["powered"]) {
            "true" -> 1
            "false" -> 0
            else -> return null
        }

        return instrument * POWERED_STATES * NOTES + note * POWERED_STATES + powered
    }

    /** Estado completo do bloco, no formato aceito por `Bukkit.createBlockData`. */
    fun blockDataString(index: Int): String = "minecraft:note_block[${propertiesOf(index)}]"
}
```

- [ ] **Passo 2: criar `BlockStateAllocator.kt`**

```kotlin
package one_block.textura.block

/**
 * Aloca variantes de blockstate de um bloco base para ids de bloco custom.
 *
 * Contrato que a fase 2 (mushroom_block, tripwire, chorus_plant) precisa
 * respeitar: uma alocação, uma vez feita, nunca muda nem é reatribuída a outro
 * id — é isso que impede um bloco já colocado no mundo de trocar de identidade
 * depois de um reload.
 */
interface BlockStateAllocator {

    /** Nome do bloco base no pack, ex.: `note_block`. */
    val baseName: String

    /** Quantos ids este alocador consegue hospedar. */
    val capacity: Int

    /**
     * Garante alocação para cada id de [ids], preservando as existentes.
     * Ids conhecidos que não estão em [ids] mantêm a reserva, mas ficam inativos.
     */
    fun sync(ids: Collection<String>)

    /** String de propriedades do id, ou `null` se ele não tem alocação ativa. */
    fun propertiesOf(id: String): String?

    /** Id dono destas propriedades, ou `null` se elas não estão alocadas ou estão inativas. */
    fun idByProperties(properties: String): String?

    /** Alocações ativas, na ordem de índice. */
    fun activeAllocations(): List<Pair<String, String>>

    /** Persiste o estado da alocação em disco. */
    fun save()
}
```

- [ ] **Passo 3: criar `NoteBlockAllocator.kt`**

```kotlin
package one_block.textura.block

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Logger

/**
 * Alocação sobre `note_block`, persistida em `generated/block-allocations.yml`.
 *
 * O arquivo é append-only: uma vez que um id entra, a linha dele nunca sai nem
 * muda de índice, mesmo que o id desapareça de `configs/blocks`. Um id removido
 * por engano volta com a mesma blockstate; um id novo sempre pega um índice
 * inédito, nunca o de outro.
 */
class NoteBlockAllocator(
    private val file: File,
    private val logger: Logger,
) : BlockStateAllocator {

    override val baseName = "note_block"
    override val capacity = NoteBlockStates.CAPACITY - NoteBlockStates.FIRST_ALLOCATABLE

    /** Todas as reservas já feitas, ativas ou não. */
    private val reserved = mutableMapOf<String, Int>()

    /** Subconjunto de [reserved] presente na última chamada de [sync]. */
    private val active = mutableSetOf<String>()

    private var nextIndex = NoteBlockStates.FIRST_ALLOCATABLE
    private var dirty = false

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!file.isFile) return

        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("reserved")
        if (section != null) {
            for (id in section.getKeys(false)) {
                val index = section.getInt(id, -1)
                if (index < NoteBlockStates.FIRST_ALLOCATABLE || index >= NoteBlockStates.CAPACITY) {
                    logger.warning("Alocacao de bloco invalida ignorada: $id -> $index")
                    continue
                }
                reserved[id] = index
            }
        }

        // O contador vive no arquivo, mas nunca pode regredir abaixo do maior
        // indice reservado — um arquivo editado a mao reatribuiria blocos.
        val salvo = config.getInt("next", NoteBlockStates.FIRST_ALLOCATABLE)
        val minimo = (reserved.values.maxOrNull() ?: (NoteBlockStates.FIRST_ALLOCATABLE - 1)) + 1
        nextIndex = maxOf(salvo, minimo, NoteBlockStates.FIRST_ALLOCATABLE)
    }

    override fun sync(ids: Collection<String>) {
        active.clear()

        for (id in ids) {
            val existente = reserved[id]
            if (existente != null) {
                active.add(id)
                continue
            }

            if (nextIndex >= NoteBlockStates.CAPACITY) {
                logger.severe(
                    "Capacidade de blockstates de note_block esgotada ($capacity slots). " +
                        "Bloco '$id' ficou sem variante e nao aparecera no pack."
                )
                continue
            }

            reserved[id] = nextIndex
            active.add(id)
            nextIndex++
            dirty = true
        }
    }

    override fun propertiesOf(id: String): String? {
        if (id !in active) return null
        val index = reserved[id] ?: return null
        return NoteBlockStates.propertiesOf(index)
    }

    override fun idByProperties(properties: String): String? {
        val index = NoteBlockStates.indexOf(properties) ?: return null
        return reserved.entries.firstOrNull { it.value == index && it.key in active }?.key
    }

    override fun activeAllocations(): List<Pair<String, String>> =
        reserved.entries
            .filter { it.key in active }
            .sortedBy { it.value }
            .map { it.key to NoteBlockStates.propertiesOf(it.value) }

    /** Índice bruto do id, ativo ou não. Usado só para diagnóstico. */
    fun indexOf(id: String): Int? = reserved[id]

    override fun save() {
        if (!dirty && file.isFile) return

        val config = YamlConfiguration()
        config.set("next", nextIndex)
        for ((id, index) in reserved.entries.sortedBy { it.value }) {
            config.set("reserved.$id", index)
        }

        runCatching {
            file.parentFile?.mkdirs()
            config.save(file)
            dirty = false
        }.onFailure {
            logger.severe("Falha ao salvar as alocacoes de bloco em ${file.path}: ${it.message}")
        }
    }
}
```

- [ ] **Passo 4: compilar**

```bash
./gradlew :plugins:textura:localJar
```

Esperado: BUILD SUCCESSFUL.

---

## Tarefa 3: geração dos modelos de bloco

**Arquivos:**
- Criar: `plugins/textura/src/main/kotlin/one_block/textura/block/BlockModelWriter.kt`

**Interfaces:**
- Consome: `BlockDefinition` (Tarefa 1).
- Produz: `BlockModelWriter.blockModelJson(definition, textureKeys): String?`, `BlockModelWriter.itemModelJson(definition): String`, `BlockModelWriter.itemDefinitionJson(definition): String`, `BlockModelWriter.blockStatesJson(allocations, fallbackModel): String`.

Os JSONs são montados como string em vez de via API do `creative` porque o formato de `blockstates` e o de `items` (1.21.4+) não têm equivalente tipado na versão usada — é o mesmo caminho que `ResourcePackBuilder.addItems` já segue.

- [ ] **Passo 1: criar `BlockModelWriter.kt`**

```kotlin
package one_block.textura.block

import one_block.textura.model.BlockDefinition

/**
 * Monta os JSONs de um bloco custom: modelo de bloco, modelo de item (o ícone
 * 3D no inventário), definição de item e o blockstate do bloco base.
 */
object BlockModelWriter {

    /**
     * Modelo do bloco a partir das faces declaradas.
     *
     * @param textureKeys face → chave da textura no pack (ex.: `oneblock:block/ruby_block`).
     *   A chave `all` significa PNG única.
     * @return o JSON, ou `null` quando o bloco traz modelo próprio (nada a gerar).
     */
    fun blockModelJson(definition: BlockDefinition, textureKeys: Map<String, String>): String? {
        if (definition.model != null) return null
        if (textureKeys.isEmpty()) return null

        val parent = when {
            textureKeys.containsKey("all") -> "minecraft:block/cube_all"
            textureKeys.keys.containsAll(listOf("top", "bottom", "side")) -> "minecraft:block/cube_bottom_top"
            else -> "minecraft:block/cube"
        }

        val entries = textureKeys.entries
            .sortedBy { it.key }
            .joinToString(",\n    ") { (face, key) -> "\"$face\": \"$key\"" }

        return """
        {
          "parent": "$parent",
          "textures": {
            $entries
          }
        }
        """.trimIndent()
    }

    /** Modelo de item: herda o modelo do bloco, dando o ícone 3D no inventário. */
    fun itemModelJson(definition: BlockDefinition): String = """
    {
      "parent": "${definition.blockModelKey}"
    }
    """.trimIndent()

    /** Definição de item que o componente `minecraft:item_model` resolve. */
    fun itemDefinitionJson(definition: BlockDefinition): String = """
    {
      "model": {
        "type": "minecraft:model",
        "model": "${definition.itemModelPath}"
      }
    }
    """.trimIndent()

    /**
     * Blockstate do bloco base com **todas** as combinações escritas.
     *
     * Escrever as 1150 explicitamente em vez de usar uma chave curinga é
     * deliberado: chaves sobrepostas em `variants` têm resolução indefinida no
     * cliente, e um estado sem variant vira modelo ausente.
     *
     * @param allocations propriedades → chave do modelo, das alocações ativas.
     * @param fallbackModel modelo de toda combinação não alocada.
     */
    fun blockStatesJson(allocations: Map<String, String>, fallbackModel: String): String {
        val entries = (0 until NoteBlockStates.CAPACITY).joinToString(",\n    ") { index ->
            val properties = NoteBlockStates.propertiesOf(index)
            val model = allocations[properties] ?: fallbackModel
            "\"$properties\": { \"model\": \"$model\" }"
        }

        return """
        {
          "variants": {
            $entries
          }
        }
        """.trimIndent()
    }
}
```

- [ ] **Passo 2: compilar**

```bash
./gradlew :plugins:textura:localJar
```

Esperado: BUILD SUCCESSFUL.

---

## Tarefa 4: escrita dos blocos no pack

**Arquivos:**
- Modificar: `plugins/textura/src/main/kotlin/one_block/textura/util/ResourcePackBuilder.kt`

**Interfaces:**
- Consome: `BlockRegistry` (T1), `NoteBlockAllocator` (T2), `BlockModelWriter` (T3), `PackAssets` (existente).
- Produz: construtor de `ResourcePackBuilder` com dois parâmetros novos (`blockRegistry`, `allocator`) e os métodos privados `addBlocks`, `addLangOverrides`.

**Ponto de atenção:** `addResourceFiles` copia tudo de `resource/` para o pack. Se um `OVERRIDE` declara `name`, o arquivo de lang precisa ser **mesclado**, não copiado. Por isso `addResourceFiles` passa a pular os arquivos de lang quando existe pelo menos um `name`, e `addLangOverrides` escreve a versão mesclada. Não confie em sobrescrever `unknownFile` no mesmo caminho.

- [ ] **Passo 1: trocar a assinatura do construtor**

Substitua o bloco de parâmetros existente por:

```kotlin
class ResourcePackBuilder(
    private val glyphRegistry: GlyphRegistry,
    private val fontRegistry: FontRegistry,
    private val titleFontRegistry: TitleFontRegistry,
    private val itemRegistry: ItemRegistry,
    private val blockRegistry: one_block.textura.registry.BlockRegistry,
    private val allocator: one_block.textura.block.NoteBlockAllocator,
    private val dataFolder: File,
    private val logger: Logger
) {
```

- [ ] **Passo 2: chamar os passos novos em `build()`**

Em `build()`, logo depois da linha `addItems(builder)`, insira:

```kotlin
        addBlocks(builder)
        addLangOverrides(builder)
```

`mergeExternalPacks(builder)` continua sendo a última chamada antes do `return`.

- [ ] **Passo 3: fazer `addResourceFiles` pular os langs sobrescritos**

Substitua o corpo de `addResourceFiles` por:

```kotlin
    private fun addResourceFiles(builder: PackBuilder) {
        // Langs com override de nome são mesclados em addLangOverrides; copiá-los
        // aqui deixaria o arquivo original no zip e perderia as chaves novas.
        val langsSobrescritos = if (temOverrideDeNome()) langPackPaths() else emptySet()

        val files = assets.resourceFiles().filterNot { it.packPath in langsSobrescritos }
        for (asset in files) {
            builder.pack.unknownFile(asset.packPath, Writable.file(asset.file))
        }
        if (files.isNotEmpty()) {
            logger.info("${files.size} arquivo(s) de resource/ copiado(s) para o pack.")
        }
    }

    private fun temOverrideDeNome(): Boolean = blockRegistry.blocks.values.any { !it.name.isNullOrBlank() }

    /** Caminhos dos `.json` de `assets/minecraft/lang/` presentes em `resource/`. */
    private fun langPackPaths(): Set<String> = assets.resourceFiles()
        .filter { it.packPath.startsWith(LANG_PREFIX) && it.packPath.endsWith(".json") }
        .map { it.packPath }
        .toSet()
```

E acrescente ao `private companion object` no fim da classe:

```kotlin
        const val LANG_PREFIX = "assets/minecraft/lang/"
        const val DEFAULT_LANG = "assets/minecraft/lang/pt_br.json"
```

- [ ] **Passo 4: implementar `addBlocks`**

Adicione o método (sugestão de posição: logo depois de `addItemModelDefinitions`):

```kotlin
    /**
     * Escreve os blocos declarados em `configs/blocks`.
     *
     * `OVERRIDE` só grava a PNG por cima do caminho vanilla. `CUSTOM` grava
     * textura, modelo de bloco, modelo de item e definição de item, e reserva
     * uma variante no blockstate do bloco base.
     */
    private fun addBlocks(builder: PackBuilder) {
        val blocksFolder = assets.configFolder("blocks")
        if (!blocksFolder.exists()) blocksFolder.mkdirs()

        val custom = blockRegistry.blocks.values.filter { it.type == BlockEntryType.CUSTOM }
        allocator.sync(custom.map { it.id })
        allocator.save()

        for (definition in blockRegistry.blocks.values) {
            when (definition.type) {
                BlockEntryType.OVERRIDE -> writeOverride(builder, blocksFolder, definition)
                BlockEntryType.CUSTOM -> writeCustomBlock(builder, blocksFolder, definition)
            }
        }

        if (custom.isNotEmpty()) writeBlockStates(builder)
    }

    /** Grava a PNG do override no caminho vanilla do bloco alvo. */
    private fun writeOverride(builder: PackBuilder, blocksFolder: File, definition: BlockDefinition) {
        val texture = definition.texture ?: return
        val packPath = "assets/minecraft/textures/block/${definition.target}.png"

        val asset = assets.resolve(texture, blocksFolder, packPath)
        if (asset == null) {
            logger.warning("Textura do bloco '${definition.id}' nao encontrada: $texture")
            return
        }

        // Já em resource/ no caminho certo: addResourceFiles cuidou dele.
        if (asset.inResource && asset.packPath == packPath) return

        builder.pack.unknownFile(packPath, Writable.file(asset.file))
    }

    private fun writeCustomBlock(builder: PackBuilder, blocksFolder: File, definition: BlockDefinition) {
        if (allocator.propertiesOf(definition.id) == null) {
            logger.warning("Bloco '${definition.id}' ficou sem blockstate alocada; nao entra no pack.")
            return
        }

        val textureKeys = writeBlockTextures(builder, blocksFolder, definition)

        val modelKey = if (definition.model != null) {
            copyDeclaredModel(builder, blocksFolder, definition) ?: return
        } else {
            val json = BlockModelWriter.blockModelJson(definition, textureKeys)
            if (json == null) {
                logger.warning("Bloco '${definition.id}': nenhuma textura utilizavel; modelo nao gerado.")
                return
            }
            builder.pack.unknownFile(
                "assets/${BlockDefinition.NAMESPACE}/models/block/${definition.id}.json",
                Writable.stringUtf8(json),
            )
            definition.blockModelKey
        }

        // O ícone do inventário herda o modelo do bloco, seja gerado ou próprio.
        val itemModel = if (modelKey == definition.blockModelKey) {
            BlockModelWriter.itemModelJson(definition)
        } else {
            """
            {
              "parent": "$modelKey"
            }
            """.trimIndent()
        }

        builder.pack.unknownFile(
            "assets/${BlockDefinition.NAMESPACE}/models/item/${definition.id}.json",
            Writable.stringUtf8(itemModel),
        )
        builder.pack.unknownFile(
            "assets/${BlockDefinition.NAMESPACE}/items/${definition.id}.json",
            Writable.stringUtf8(BlockModelWriter.itemDefinitionJson(definition)),
        )
    }

    /** Grava as PNGs do bloco e devolve `face -> chave da textura no pack`. */
    private fun writeBlockTextures(
        builder: PackBuilder,
        blocksFolder: File,
        definition: BlockDefinition,
    ): Map<String, String> {
        val declaradas = if (definition.texture != null) {
            mapOf("all" to definition.texture)
        } else {
            definition.textures
        }

        val resultado = mutableMapOf<String, String>()
        for ((face, caminho) in declaradas) {
            val sufixo = if (face == "all") definition.id else "${definition.id}_$face"
            val packPath = "assets/${BlockDefinition.NAMESPACE}/textures/block/$sufixo.png"

            val asset = assets.resolve(caminho, blocksFolder, packPath)
            if (asset == null) {
                logger.warning("Bloco '${definition.id}': textura da face '$face' nao encontrada: $caminho")
                continue
            }

            if (!asset.inResource) builder.pack.unknownFile(asset.packPath, Writable.file(asset.file))
            resultado[face] = asset.key.asString()
        }
        return resultado
    }

    /** Copia o JSON de modelo declarado e devolve a chave dele. */
    private fun copyDeclaredModel(
        builder: PackBuilder,
        blocksFolder: File,
        definition: BlockDefinition,
    ): String? {
        val modelPath = definition.model ?: return null
        val packPath = "assets/${BlockDefinition.NAMESPACE}/models/block/${definition.id}.json"

        val asset = assets.resolve(modelPath, blocksFolder, packPath)
        if (asset == null) {
            logger.warning("Bloco '${definition.id}': modelo nao encontrado: $modelPath")
            return null
        }

        if (!asset.inResource) builder.pack.unknownFile(asset.packPath, Writable.file(asset.file))
        return asset.key.asString()
    }

    private fun writeBlockStates(builder: PackBuilder) {
        val allocations = allocator.activeAllocations().associate { (id, properties) ->
            properties to "${BlockDefinition.NAMESPACE}:block/$id"
        }

        val json = BlockModelWriter.blockStatesJson(allocations, "minecraft:block/note_block")
        builder.pack.unknownFile("assets/minecraft/blockstates/note_block.json", Writable.stringUtf8(json))

        logger.info("${allocations.size} bloco(s) custom alocado(s) em variantes de note_block.")
    }
```

- [ ] **Passo 5: implementar `addLangOverrides`**

```kotlin
    /**
     * Mescla os nomes declarados nos blocos dentro dos arquivos de lang.
     *
     * As chaves do yml vencem as do arquivo. Quando `resource/` não tem nenhum
     * lang, um `pt_br.json` novo é criado só com as chaves dos blocos.
     */
    private fun addLangOverrides(builder: PackBuilder) {
        val nomes = blockRegistry.blocks.values
            .filter { !it.name.isNullOrBlank() }
            .associate { it.translationKey to (it.name ?: "") }
        if (nomes.isEmpty()) return

        val langs = assets.resourceFiles()
            .filter { it.packPath.startsWith(LANG_PREFIX) && it.packPath.endsWith(".json") }

        if (langs.isEmpty()) {
            builder.pack.unknownFile(DEFAULT_LANG, Writable.stringUtf8(GSON.toJson(nomes)))
            return
        }

        for (lang in langs) {
            val existente = runCatching {
                FileReader(lang.file).use { reader ->
                    JsonParser.parseReader(reader).asJsonObject
                        .entrySet()
                        .associate { it.key to it.value.asString }
                }
            }.getOrElse {
                logger.warning("Lang ${lang.packPath} ilegivel (${it.message}); sera reescrito so com os nomes de bloco.")
                emptyMap()
            }

            val mesclado = existente + nomes
            builder.pack.unknownFile(lang.packPath, Writable.stringUtf8(GSON.toJson(mesclado)))
        }

        logger.info("${nomes.size} nome(s) de bloco mesclado(s) em ${langs.size} arquivo(s) de lang.")
    }
```

- [ ] **Passo 6: adicionar os imports que faltam**

No topo de `ResourcePackBuilder.kt`, acrescente:

```kotlin
import com.google.gson.Gson
import one_block.textura.block.BlockModelWriter
import one_block.textura.model.BlockDefinition
import one_block.textura.model.BlockEntryType
```

E no `private companion object`, junto com as constantes do Passo 3:

```kotlin
        val GSON: Gson = com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
```

`disableHtmlEscaping` é obrigatório: sem ele o Gson transforma acento e `<` em escape unicode dentro do lang.

- [ ] **Passo 7: compilar**

```bash
./gradlew :plugins:textura:localJar
```

Esperado: falha em `TexturaPlugin.kt`, porque o construtor de `ResourcePackBuilder` mudou e ainda é chamado com os parâmetros antigos. Isso é esperado — a Tarefa 5 corrige.

---

## Tarefa 5: API e wiring do Textura

**Arquivos:**
- Criar: `plugins/textura/src/main/kotlin/one_block/textura/api/BlockApi.kt`
- Modificar: `plugins/textura/src/main/kotlin/one_block/textura/TexturaPlugin.kt`

**Interfaces:**
- Consome: `BlockRegistry` (T1), `NoteBlockAllocator` (T2).
- Produz: `BlockApi.init`, `has`, `get`, `ids`, `blockData`, `idAt`, `itemModelKey`, `itemMaterial`.

- [ ] **Passo 1: criar `BlockApi.kt`**

```kotlin
package one_block.textura.api

import one_block.textura.block.NoteBlockAllocator
import one_block.textura.model.BlockDefinition
import one_block.textura.model.BlockEntryType
import one_block.textura.registry.BlockRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block

/**
 * Acesso aos blocos registrados no Textura a partir de outros plugins.
 *
 * A identidade de um bloco custom colocado vive na própria blockstate — [idAt]
 * resolve lendo o bloco do mundo, sem consultar banco nenhum.
 *
 * As referências são `@Volatile` porque `/textura reload` troca os registries
 * vivos por baixo de quem estiver consultando.
 */
object BlockApi {

    @Volatile
    private var registry: BlockRegistry? = null

    @Volatile
    private var allocator: NoteBlockAllocator? = null

    fun init(registry: BlockRegistry, allocator: NoteBlockAllocator) {
        this.registry = registry
        this.allocator = allocator
    }

    fun has(id: String): Boolean = registry?.blocks?.containsKey(id) == true

    fun get(id: String): BlockDefinition? = registry?.blocks?.get(id)

    fun ids(): Set<String> = registry?.blocks?.keys.orEmpty()

    /** Ids dos blocos `CUSTOM` (os que existem no mundo com blockstate própria). */
    fun customIds(): Set<String> =
        registry?.blocks?.values?.filter { it.type == BlockEntryType.CUSTOM }?.map { it.id }?.toSet().orEmpty()

    /**
     * BlockData a colocar no mundo, ou `null` se o id não existe, não é `CUSTOM`,
     * ou ficou sem alocação.
     */
    fun blockData(id: String): org.bukkit.block.data.BlockData? {
        val definition = get(id) ?: return null
        if (definition.type != BlockEntryType.CUSTOM) return null
        val properties = allocator?.propertiesOf(id) ?: return null

        return runCatching { Bukkit.createBlockData("minecraft:note_block[$properties]") }.getOrNull()
    }

    /**
     * Id do bloco custom nesta posição, ou `null` se for bloco comum.
     *
     * Lê as propriedades da string do BlockData em vez do enum `Instrument` do
     * Bukkit: os nomes do enum divergem dos nomes do blockstate (`PIANO` vs
     * `harp`) e a string é o que o pack usa.
     */
    fun idAt(block: Block): String? {
        if (block.type != Material.NOTE_BLOCK) return null
        val properties = block.blockData.asString
            .substringAfter('[', "")
            .substringBefore(']')
        if (properties.isEmpty()) return null

        return allocator?.idByProperties(properties)
    }

    /** Chave do componente `minecraft:item_model` do item que representa o bloco. */
    fun itemModelKey(id: String): NamespacedKey? {
        val definition = get(id) ?: return null
        if (definition.type != BlockEntryType.CUSTOM) return null
        return NamespacedKey(BlockDefinition.NAMESPACE, definition.id)
    }

    /** Material base do item que representa o bloco no inventário. */
    fun itemMaterial(id: String): Material? {
        val definition = get(id) ?: return null
        return Material.matchMaterial(definition.itemMaterial)
    }
}
```

- [ ] **Passo 2: declarar os campos novos em `TexturaPlugin`**

Depois do campo `itemRegistry`, acrescente:

```kotlin
    lateinit var blockRegistry: one_block.textura.registry.BlockRegistry
        private set
    lateinit var blockAllocator: one_block.textura.block.NoteBlockAllocator
        private set
```

- [ ] **Passo 3: instanciar em `onEnable`**

Logo depois de `itemRegistry = ItemRegistry(logger)`:

```kotlin
        blockRegistry = one_block.textura.registry.BlockRegistry(logger)
        blockAllocator = one_block.textura.block.NoteBlockAllocator(
            File(dataFolder, "generated/block-allocations.yml"),
            logger,
        )
        one_block.textura.api.BlockApi.init(blockRegistry, blockAllocator)
```

E junto das outras chamadas de `mkdirs()`:

```kotlin
        File(dataFolder, "configs/blocks").mkdirs()
```

- [ ] **Passo 4: carregar no reload**

Em `reloadConfigData()`, depois de `itemRegistry.load(...)`:

```kotlin
        blockRegistry.load(assets.configFolder("blocks"))
```

- [ ] **Passo 5: passar os parâmetros novos ao builder**

Em `generatePack`, troque a linha do builder por:

```kotlin
                val builder = ResourcePackBuilder(
                    glyphRegistry, fontRegistry, titleFontRegistry, itemRegistry,
                    blockRegistry, blockAllocator, dataFolder, logger,
                )
```

E na mensagem de resultado, depois da linha de `Items`:

```kotlin
                    sender.sendMessage("§7Blocos: ${blockRegistry.blocks.size}")
```

- [ ] **Passo 6: compilar**

```bash
./gradlew :plugins:textura:localJar
```

Esperado: BUILD SUCCESSFUL.

- [ ] **Passo 7: criar um yml de exemplo para teste manual**

Crie `server/plugins/Textura/configs/blocks/exemplo.yml`:

```yaml
# Retextura um bloco vanilla. Vale no servidor inteiro.
ruby_ore:
  type: OVERRIDE
  target: deepslate_emerald_ore
  texture: block/ruby_ore.png
  name: "Minério de Rubi"

# Bloco novo, sobre uma variante de note_block.
ruby_block:
  type: CUSTOM
  base: NOTE_BLOCK
  texture: block/ruby_block.png
  name: "Bloco de Rubi"
  item_material: paper
```

As PNGs vão em `server/plugins/Textura/configs/blocks/block/ruby_ore.png` e `.../ruby_block.png`. Com o servidor rodando, `/textura gerar` e confira em `server/plugins/Textura/generated/resourcepack.zip` que existem `assets/minecraft/blockstates/note_block.json`, `assets/oneblock/models/block/ruby_block.json` e `assets/minecraft/textures/block/deepslate_emerald_ore.png`.

---

## Tarefa 6: `CustomBlock` com visual custom

**Arquivos:**
- Modificar: `plugins/oneblock/system/custom-items/build.gradle.kts`
- Modificar: `.../custom_items/block/CustomBlock.kt`
- Modificar: `.../custom_items/block/CustomBlockRegistry.kt`

**Interfaces:**
- Consome: `BlockApi` (T5).
- Produz: `CustomBlock.texturaBlockId: String?`, `CustomBlock.stateful: Boolean`, `CustomBlockRegistry.byTexturaId(String): CustomBlock?`.

- [ ] **Passo 1: adicionar a dependência do Textura**

Em `build.gradle.kts`, no bloco `dependencies`, junto dos outros `compileOnly(projects...)`:

```kotlin
    compileOnly(projects.plugins.textura)
```

- [ ] **Passo 2: adicionar os campos em `CustomBlock`**

Depois de `open val itemModel: NamespacedKey? = null`:

```kotlin
    /**
     * Id de um bloco `CUSTOM` do Textura. Quando presente, o bloco ganha
     * aparência própria no mundo (uma variante de blockstate de note_block) e o
     * item passa a usar o material e o `item_model` declarados no Textura, em
     * vez de [material] e [itemModel].
     */
    open val texturaBlockId: String? = null

    /**
     * `false` para blocos puramente decorativos: nada é gravado no Mongo e o
     * limite por ilha não é checado, porque a identidade já vive na blockstate.
     * Blocos com armazenamento, upgrades ou filtro precisam de `true`.
     */
    open val stateful: Boolean = true
```

- [ ] **Passo 3: fazer `buildItem` respeitar o visual do Textura**

Substitua o início de `buildItem` (a linha `open fun buildItem(...) = itemStack(material) {` e as duas primeiras linhas do corpo) por:

```kotlin
    open fun buildItem(placed: PlacedBlock? = null, amount: Int = 1): ItemStack {
        val texturaId = texturaBlockId
        val baseMaterial = texturaId?.let { BlockApi.itemMaterial(it) } ?: material
        val modelKey = texturaId?.let { BlockApi.itemModelKey(it) } ?: itemModel

        return itemStack(baseMaterial) {
            this.amount = amount
            meta<ItemMeta> {
                modelKey?.let { model -> itemModel = model }
                name(this@CustomBlock.displayName)
```

O restante do corpo (o `setLore { ... }`, o `nbt { ... }` e o `placed?.let { ... }`) fica igual. Feche com `}` extra no fim do método, já que ele deixou de ser expressão.

Acrescente o import no topo do arquivo:

```kotlin
import one_block.textura.api.BlockApi
```

- [ ] **Passo 4: adicionar o índice reverso em `CustomBlockRegistry`**

Substitua o corpo do objeto por:

```kotlin
object CustomBlockRegistry {

    private val blocks = mutableMapOf<String, CustomBlock>()

    /** `texturaBlockId -> bloco`. É o caminho de resolução na quebra. */
    private val byTextura = mutableMapOf<String, CustomBlock>()

    fun register(block: CustomBlock) {
        if (blocks.containsKey(block.id)) {
            Instance.logger.warning("[custom-items] id de bloco duplicado ignorado: ${block.id}")
            return
        }
        blocks[block.id] = block

        val texturaId = block.texturaBlockId ?: return
        val anterior = byTextura[texturaId]
        if (anterior != null) {
            Instance.logger.warning(
                "[custom-items] blocos '${anterior.id}' e '${block.id}' apontam para o mesmo " +
                    "texturaBlockId '$texturaId'; o segundo nao sera resolvido na quebra."
            )
            return
        }
        byTextura[texturaId] = block
    }

    fun get(id: String): CustomBlock? = blocks[id]

    /** Bloco dono deste id do Textura, ou `null`. */
    fun byTexturaId(texturaId: String): CustomBlock? = byTextura[texturaId]

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

- [ ] **Passo 5: compilar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL.

---

## Tarefa 7: colocar e quebrar bloco com visual custom

**Arquivos:**
- Modificar: `.../custom_items/block/CustomBlockListener.kt`

**Interfaces:**
- Consome: `BlockApi.blockData`, `BlockApi.idAt` (T5); `CustomBlock.texturaBlockId`, `CustomBlock.stateful`, `CustomBlockRegistry.byTexturaId` (T6).
- Produz: nenhum símbolo novo para outras tarefas.

- [ ] **Passo 1: aplicar a blockstate em `onPlace`**

Em `onPlace`, entre o bloco do limite por ilha e a linha `val location = PlacedBlockManager.keyOf(block)`, insira:

```kotlin
        // A aparência vem antes de qualquer registro: se a blockstate não puder
        // ser aplicada, o bloco não deve existir no mundo com cara de note block.
        val texturaId = custom.texturaBlockId
        if (texturaId != null) {
            val data = BlockApi.blockData(texturaId)
            if (data == null) {
                event.isCancelled = true
                player.sendRichMessage("<red>Este bloco esta mal configurado no resource pack.")
                Instance.logger.warning(
                    "[custom-items] bloco '${custom.id}' aponta para o texturaBlockId " +
                        "'$texturaId', que nao tem blockstate no Textura."
                )
                return
            }
            block.setBlockData(data, false)
        }
```

E troque o bloco do limite por ilha para pular blocos sem estado:

```kotlin
        if (custom.stateful) {
            val current = PlacedBlockManager.countOnIsland(custom.id, island.uuid)
            if (current >= custom.limitPerIsland) {
                event.isCancelled = true
                player.sendRichMessage("<red>Limite de <white>${custom.limitPerIsland}<red> por ilha atingido.")
                return
            }
        }
```

Como `current` deixou de existir fora do `if`, substitua as duas últimas linhas do método por:

```kotlin
        if (!custom.stateful) {
            runHook(custom.id) { custom.onPlace(PlaceContext(player, block, placed, event)) }
            return
        }

        runHook(custom.id) { custom.onPlace(PlaceContext(player, block, placed, event)) }
        if (event.isCancelled) return

        PlacedBlockManager.register(placed)
        val total = PlacedBlockManager.countOnIsland(custom.id, island.uuid)
        player.sendRichMessage("<green>Bloco colocado. <gray>($total/${custom.limitPerIsland})")
```

- [ ] **Passo 2: resolver a quebra por blockstate**

Substitua a primeira parte de `onBreak` (da linha `val placed = PlacedBlockManager.get(block) ?: return` até o fechamento do `?: run { ... }`) por:

```kotlin
        val placed = PlacedBlockManager.get(block)

        // Bloco sem estado no Mongo ainda pode ser custom: a identidade dele vive
        // na blockstate. Esse é o caminho dos blocos decorativos e de minério.
        val custom = if (placed != null) {
            CustomBlockRegistry.get(placed.blockId) ?: run {
                Instance.logger.warning(
                    "[custom-items] (break) bloco ${placed.uuid} em ${placed.location} " +
                        "tem blockId '${placed.blockId}' sem CustomBlock registrado."
                )
                return
            }
        } else {
            val texturaId = BlockApi.idAt(block) ?: return
            CustomBlockRegistry.byTexturaId(texturaId) ?: return
        }
```

- [ ] **Passo 3: ajustar o fim de `onBreak`**

Substitua o trecho final (de `event.isDropItems = false` até o `dropItemNaturally`) por:

```kotlin
        // Sem isto o Bukkit dropa também o bloco vanilla e duplica material.
        event.isDropItems = false
        placed?.let { PlacedBlockManager.unregister(it.location) }

        val dropAt = block.location.clone().add(0.5, 0.5, 0.5)
        block.world.dropItemNaturally(dropAt, custom.buildItem(placed))
```

E o `runHook` acima dele passa a ser:

```kotlin
        runHook(custom.id) { custom.onBreak(BreakContext(player, block, placed, event)) }
        if (event.isCancelled) return
```

`BreakContext` precisa aceitar `placed` nulo — abra `block/context/BreakContext.kt` e troque o tipo do campo `placed` de `PlacedBlock` para `PlacedBlock?`. Faça a mesma troca em `PlaceContext.kt` e `BlockClickContext.kt` **apenas se** o compilador reclamar; `onPlace` e `onRightClick` continuam sempre com um `placed` real.

- [ ] **Passo 4: resolver o clique direito por blockstate também**

Em `onInteract`, substitua as duas linhas de resolução por:

```kotlin
        val placed = PlacedBlockManager.get(block)
        val custom = if (placed != null) {
            CustomBlockRegistry.get(placed.blockId) ?: return
        } else {
            val texturaId = BlockApi.idAt(block) ?: return
            CustomBlockRegistry.byTexturaId(texturaId) ?: return
        }
```

- [ ] **Passo 5: adicionar os imports**

No topo do arquivo:

```kotlin
import one_block.textura.api.BlockApi
```

- [ ] **Passo 6: compilar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL. Se o compilador apontar `placed` não-nulo dentro dos contexts, aplique a troca para `PlacedBlock?` descrita no Passo 3.

---

## Tarefa 8: `NoteBlockGuard`

**Arquivos:**
- Criar: `.../custom_items/block/NoteBlockGuard.kt`

**Interfaces:**
- Consome: `BlockApi.idAt` (T5).
- Produz: `class NoteBlockGuard : Listener`.

Sem este listener o sistema se corrompe sozinho: qualquer update de bloco faz o servidor recalcular `instrument` a partir do bloco de baixo, e o bloco custom vira outro (ou some do registro).

- [ ] **Passo 1: criar `NoteBlockGuard.kt`**

```kotlin
package one_block.system.custom_items.block

import one_block.textura.api.BlockApi
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * Mantém a identidade dos blocos custom hospedados em `note_block`.
 *
 * O vanilla trata as três propriedades que codificam a identidade como estado
 * mutável de gameplay: `instrument` é recalculado a cada update de bloco a
 * partir do bloco de baixo, `note` é incrementada por clique direito e
 * `powered` acompanha a redstone. Qualquer um dos três mudando transforma o
 * bloco em outro id — ou em nenhum.
 *
 * Os quatro cancelamentos abaixo são o que impede isso. Todos são restritos aos
 * note blocks que o Textura reconhece: note block vanilla continua tocando e
 * respondendo normalmente.
 */
class NoteBlockGuard : Listener {

    /** Silencia o bloco custom. */
    @EventHandler(ignoreCancelled = true)
    fun onNotePlay(event: NotePlayEvent) {
        if (BlockApi.idAt(event.block) == null) return
        event.isCancelled = true
    }

    /**
     * O update de física é o caso mais perigoso: sem cancelar, colocar ou
     * quebrar qualquer bloco embaixo do custom troca o `instrument` e o bloco
     * muda de identidade sem ninguém tocar nele.
     */
    @EventHandler(ignoreCancelled = true)
    fun onPhysics(event: BlockPhysicsEvent) {
        if (event.block.type != Material.NOTE_BLOCK) return
        if (BlockApi.idAt(event.block) == null) return
        event.isCancelled = true
    }

    /**
     * Clique direito incrementa a `note` no vanilla. Cancela só o efeito
     * vanilla — o `CustomBlockListener.onInteract` continua rodando o
     * `onRightClick` do bloco, porque ele tem prioridade normal e este handler
     * não consome o evento para os outros listeners.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.NOTE_BLOCK) return
        if (BlockApi.idAt(block) == null) return

        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY)
    }

    /** Redstone adjacente flipa `powered`, que faz parte da identidade. */
    @EventHandler
    fun onRedstone(event: BlockRedstoneEvent) {
        if (event.block.type != Material.NOTE_BLOCK) return
        if (BlockApi.idAt(event.block) == null) return
        event.newCurrent = event.oldCurrent
    }
}
```

`setUseInteractedBlock(DENY)` em vez de `isCancelled = true`: cancelar o evento inteiro também bloquearia o item na mão, e o `CustomBlockListener` verifica `ignoreCancelled`.

- [ ] **Passo 2: compilar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL. O listener ainda não está registrado — a Tarefa 10 faz isso.

---

## Tarefa 9: override de bloco vanilla

**Arquivos:**
- Criar: `.../custom_items/block/BlockBreakEffects.kt`
- Criar: `.../custom_items/block/VanillaOverride.kt`
- Criar: `.../custom_items/block/VanillaOverrideRegistry.kt`
- Criar: `.../custom_items/block/VanillaOverrideListener.kt`

**Interfaces:**
- Consome: nada das tarefas anteriores.
- Produz: `VanillaOverride` (data class), `VanillaOverrideRegistry.register/get/fortuneMultiplier`, `BlockBreakEffects.apply`, `class VanillaOverrideListener : Listener`.

- [ ] **Passo 1: criar `VanillaOverride.kt`**

```kotlin
package one_block.system.custom_items.block

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack

/**
 * Substituição de comportamento de um bloco vanilla retexturado no Textura.
 *
 * O visual é responsabilidade do Textura (entrada `type: OVERRIDE` no yml); o
 * que fica aqui é só o que o resource pack não consegue fazer: drop próprio,
 * som e partícula.
 */
data class VanillaOverride(
    val target: Material,
    /**
     * Itens dropados. Recebe o nível de fortuna já lido da ferramenta; a
     * multiplicação em si é responsabilidade de quem registra, via
     * [VanillaOverrideRegistry.fortuneMultiplier].
     */
    val drop: (fortuneLevel: Int) -> List<ItemStack>,
    val sound: Sound? = null,
    /**
     * Material vanilla cuja textura a partícula de quebra usa. Partícula de
     * bloco não aceita textura custom no cliente, então o melhor possível é um
     * parecido.
     */
    val particleLookalike: Material? = null,
    /**
     * `true` cancela a quebra vanilla e reproduz tudo à mão: só o som e a
     * partícula custom aparecem, mas perde-se dano de ferramenta, XP e
     * estatística. `false` (padrão) deixa o vanilla quebrar e só troca o drop,
     * somando som e partícula por cima.
     */
    val replaceEffects: Boolean = false,
)
```

- [ ] **Passo 2: criar `VanillaOverrideRegistry.kt`**

```kotlin
package one_block.system.custom_items.block

import one_block.libs.instance.Instance
import org.bukkit.Material
import kotlin.random.Random

object VanillaOverrideRegistry {

    private val overrides = mutableMapOf<Material, VanillaOverride>()

    fun register(override: VanillaOverride) {
        if (overrides.containsKey(override.target)) {
            Instance.logger.warning(
                "[custom-items] override duplicado para ${override.target}; o segundo foi ignorado."
            )
            return
        }
        overrides[override.target] = override
    }

    fun get(material: Material): VanillaOverride? = overrides[material]

    fun targets(): Set<Material> = overrides.keys.toSet()

    /**
     * Multiplicador de drop da Fortuna, com a fórmula de `apply_bonus`/`ore_drops`
     * do vanilla.
     *
     * O sorteio vai de `-1` a `level`, e o clamp em 0 é o que dobra a chance do
     * 1×: com Fortuna III, 1× sai em 2/5 dos casos e 2×, 3× e 4× em 1/5 cada.
     */
    fun fortuneMultiplier(level: Int): Int {
        if (level <= 0) return 1
        val sorteio = Random.nextInt(level + 2) - 1
        return if (sorteio < 0) 1 else sorteio + 1
    }
}
```

- [ ] **Passo 3: criar `BlockBreakEffects.kt`**

```kotlin
package one_block.system.custom_items.block

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack

/**
 * Caminho compartilhado de quebra com drop, som e partícula próprios.
 *
 * Dois modos, porque não dá para ter os dois lados: o aditivo preserva tudo que
 * o vanilla faz na quebra (dano na ferramenta, XP, estatística) mas deixa o som
 * vanilla tocar junto; o substitutivo dá controle total do feel e abre mão
 * daquelas mecânicas.
 */
object BlockBreakEffects {

    fun apply(
        event: BlockBreakEvent,
        drops: List<ItemStack>,
        sound: Sound?,
        particleLookalike: Material?,
        replaceEffects: Boolean,
    ) {
        val block = event.block
        val center = block.location.toCenterLocation()

        if (replaceEffects) {
            event.isCancelled = true
            // `false` para não disparar update de física nos vizinhos — é o que
            // impede um bloco custom vizinho de recalcular a própria blockstate.
            block.setType(Material.AIR, false)
        } else {
            event.isDropItems = false
        }

        for (drop in drops) {
            if (drop.type.isAir || drop.amount <= 0) continue
            block.world.dropItemNaturally(center, drop)
        }

        sound?.let { block.world.playSound(center, it, 1.0f, 1.0f) }

        particleLookalike?.let { lookalike ->
            block.world.spawnParticle(
                Particle.BLOCK,
                center,
                PARTICLE_COUNT,
                PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD,
                0.0,
                lookalike.createBlockData(),
            )
        }
    }

    /** Como o vanilla: partícula de quebra sai em nuvem, não em ponto. */
    private const val PARTICLE_COUNT = 30
    private const val PARTICLE_SPREAD = 0.3
}
```

- [ ] **Passo 4: criar `VanillaOverrideListener.kt`**

```kotlin
package one_block.system.custom_items.block

import one_block.libs.instance.Instance
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

/**
 * Aplica os overrides de bloco vanilla na quebra.
 *
 * Roda depois do [CustomBlockListener] na cadeia de prioridade padrão, e nunca
 * disputa com ele: os materiais registrados aqui são blocos vanilla, não
 * note_block.
 */
class VanillaOverrideListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val override = VanillaOverrideRegistry.get(event.block.type) ?: return

        val fortune = event.player.inventory.itemInMainHand
            .getEnchantmentLevel(Enchantment.FORTUNE)

        val drops = runCatching { override.drop(fortune) }.getOrElse {
            Instance.logger.warning(
                "[custom-items] drop do override de ${override.target} falhou: ${it.message}"
            )
            return
        }

        BlockBreakEffects.apply(
            event = event,
            drops = drops,
            sound = override.sound,
            particleLookalike = override.particleLookalike,
            replaceEffects = override.replaceEffects,
        )
    }
}
```

**Silk Touch é deliberadamente ignorado:** não há checagem de encantamento além da Fortuna, então quebrar com Silk Touch dropa o item custom igual a quebrar sem.

- [ ] **Passo 5: compilar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL. Se `Enchantment.FORTUNE` não resolver, verifique o nome na paper-api da versão em uso — a constante foi renomeada de `LOOT_BONUS_BLOCKS` em versões anteriores.

---

## Tarefa 10: wiring no `CustomItemSystem`

**Arquivos:**
- Modificar: `.../custom_items/CustomItemSystem.kt`

**Interfaces:**
- Consome: `NoteBlockGuard` (T8), `VanillaOverrideListener`, `VanillaOverrideRegistry`, `VanillaOverride` (T9).
- Produz: nenhum símbolo novo.

- [ ] **Passo 1: registrar os dois listeners**

Em `init`, depois da linha que registra o `CustomBlockListener`:

```kotlin
        plugin.server.pluginManager.registerEvents(NoteBlockGuard(), plugin)
        plugin.server.pluginManager.registerEvents(VanillaOverrideListener(), plugin)
```

- [ ] **Passo 2: registrar o override de exemplo**

Adicione o método e chame-o em `init`, logo depois de `registerBuiltInBlocks(...)`:

```kotlin
    /**
     * Overrides de bloco vanilla. O visual de cada um vem de uma entrada
     * `type: OVERRIDE` em `configs/blocks` do Textura; aqui fica só o
     * comportamento.
     */
    private fun registerVanillaOverrides() {
        VanillaOverrideRegistry.register(
            VanillaOverride(
                target = Material.DEEPSLATE_EMERALD_ORE,
                drop = { fortune ->
                    val quantidade = VanillaOverrideRegistry.fortuneMultiplier(fortune)
                    listOf(ItemStack(Material.EMERALD, quantidade))
                },
                sound = Sound.BLOCK_AMETHYST_BLOCK_BREAK,
                particleLookalike = Material.DEEPSLATE_EMERALD_ORE,
            )
        )
    }
```

O drop acima é o placeholder mínimo para verificar o caminho ponta a ponta; troque `ItemStack(Material.EMERALD, ...)` pelo item de rubi real quando ele existir.

- [ ] **Passo 3: adicionar os imports**

```kotlin
import one_block.system.custom_items.block.NoteBlockGuard
import one_block.system.custom_items.block.VanillaOverride
import one_block.system.custom_items.block.VanillaOverrideListener
import one_block.system.custom_items.block.VanillaOverrideRegistry
import org.bukkit.Material
import org.bukkit.Sound
```

`org.bukkit.inventory.ItemStack` já está importado.

- [ ] **Passo 4: compilar e implantar**

```bash
./gradlew :plugins:oneblock:plugin:localDeploy
```

Esperado: BUILD SUCCESSFUL, com o jar copiado para `server/plugins/`. O servidor precisa estar parado antes — o Windows trava o jar em uso.

- [ ] **Passo 5: verificação manual no servidor**

Suba o servidor e confira, nesta ordem:

1. O console mostra `Foram carregados N bloco(s) custom e M override(s)` e `N bloco(s) custom alocado(s) em variantes de note_block`.
2. `server/plugins/Textura/generated/block-allocations.yml` existe e tem uma linha por bloco `CUSTOM`.
3. Um `DEEPSLATE_EMERALD_ORE` colocado aparece com a textura de rubi e o nome novo no tooltip.
4. Quebrar esse minério dropa o item configurado e a quantidade varia com Fortuna.
5. Colocar um bloco custom (via o `CustomBlockCommand` existente) mostra o modelo próprio, não um note block.
6. Clicar com botão direito nele **não** muda o som nem a aparência.
7. Quebrar e recolocar o bloco embaixo dele **não** muda a aparência do bloco custom. Este é o teste que valida o `BlockPhysicsEvent`.
8. Um note block vanilla comum continua tocando ao clicar.

---

## Notas de revisão

- **Cobertura do spec:** configuração (T1), alocação (T2), modelos (T3), escrita no pack e lang (T4), API e reload (T5), `texturaBlockId`/`stateful` (T6), colocar/quebrar por blockstate (T7), as quatro guardas (T8), som/partícula/drop com Fortuna e Silk Touch ignorado (T9), wiring (T10).
- **Ordem obrigatória.** T4 deixa a compilação quebrada de propósito até T5; não pule entre as duas.
- **Fase 2 não está aqui.** `mushroom_block`, `tripwire` e `chorus_plant` entram implementando `BlockStateAllocator` e trocando o ponto único onde `NoteBlockAllocator` é instanciado no `TexturaPlugin`.
- **Limitação conhecida:** blocos com `stateful = false` não são protegidos de explosão pelo `CustomBlockListener.protect`, porque essa proteção depende do registro no Mongo. Explodir um bloco decorativo o destrói sem drop custom. Se isso incomodar, o caminho é checar `BlockApi.idAt` também em `protect`.
