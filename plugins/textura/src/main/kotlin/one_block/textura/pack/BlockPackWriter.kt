package one_block.textura.pack

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import one_block.textura.block.BlockModelWriter
import one_block.textura.block.NoteBlockAllocator
import one_block.textura.model.BlockDefinition
import one_block.textura.model.BlockEntryType
import one_block.textura.registry.BlockRegistry
import team.unnamed.creative.base.Writable
import java.io.File
import java.io.FileReader
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Grava os blocos no pack: texturas e modelos de cada definição, o
 * `blockstates/note_block.json` com as variantes alocadas e os nomes traduzidos.
 */
class BlockPackWriter(
    private val blockRegistry: BlockRegistry,
    private val allocator: NoteBlockAllocator,
    private val assets: PackAssets,
    private val logger: Logger,
) {

    /** `true` quando algum bloco define `name`, o que faz os langs serem mesclados. */
    fun temOverrideDeNome(): Boolean = blockRegistry.blocks.values.any { !it.name.isNullOrBlank() }

    fun write(builder: PackBuilder) {
        val blocksFolder = assets.configFolder("blocks")
        if (!blocksFolder.exists()) blocksFolder.mkdirs()

        val custom = blockRegistry.blocks.values.filter { it.type == BlockEntryType.CUSTOM }

        for (definition in blockRegistry.blocks.values) isolated(definition.id) {
            when (definition.type) {
                BlockEntryType.OVERRIDE -> writeOverride(builder, blocksFolder, definition)
                BlockEntryType.CUSTOM -> writeCustomBlock(builder, blocksFolder, definition)
            }
        }

        if (custom.isNotEmpty()) writeBlockStates(builder)
    }

    private fun writeOverride(builder: PackBuilder, blocksFolder: File, definition: BlockDefinition) {
        val texture = definition.texture ?: return
        val destino = "assets/minecraft/textures/block/${definition.target}.png"
        val origem = "assets/${BlockDefinition.NAMESPACE}/textures/block/${definition.id}.png"

        val asset = assets.resolve(texture, blocksFolder, origem)
            ?: assets.resolve(texture, blocksFolder, destino)
        if (asset == null) {
            logger.warning("Textura do bloco '${definition.id}' nao encontrada: $texture")
            return
        }

        if (asset.inResource && asset.packPath == destino) return

        builder.pack.unknownFile(destino, Writable.file(asset.file))
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

    fun writeLangOverrides(builder: PackBuilder) {
        val nomes = blockRegistry.blocks.values
            .filter { !it.name.isNullOrBlank() }
            .associate { it.translationKey to (it.name ?: "") }
        if (nomes.isEmpty()) return

        val langs = assets.langFiles()

        if (langs.isEmpty()) {
            builder.pack.unknownFile(PackAssets.DEFAULT_LANG, Writable.stringUtf8(GSON.toJson(nomes)))
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

    private companion object {
        val GSON: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    }

    private inline fun isolated(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Bloco '$what' ignorado no pack", e)
        }
    }
}
