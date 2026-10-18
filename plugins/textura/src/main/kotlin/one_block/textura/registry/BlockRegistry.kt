package one_block.textura.registry

import one_block.textura.model.BlockBase
import one_block.textura.model.BlockDefinition
import one_block.textura.model.BlockEntryType
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Logger

class BlockRegistry(private val logger: Logger) {

    @Volatile
    var blocks: Map<String, BlockDefinition> = emptyMap()
        private set

    @Volatile
    var overridesByTarget: Map<String, BlockDefinition> = emptyMap()
        private set

    @Volatile
    var customIds: Set<String> = emptySet()
        private set

    fun load(blocksFolder: File) {
        if (!blocksFolder.exists()) {
            blocksFolder.mkdirs()
            blocks = emptyMap()
            overridesByTarget = emptyMap()
            customIds = emptySet()
            return
        }

        val loaded = linkedMapOf<String, BlockDefinition>()
        YamlFiles.collect(blocksFolder).forEach { loadYaml(it, loaded) }

        overridesByTarget = loaded.values
            .filter { it.type == BlockEntryType.OVERRIDE }
            .mapNotNull { definition -> definition.target?.let { it to definition } }
            .toMap()
        customIds = loaded.values.filter { it.type == BlockEntryType.CUSTOM }.map { it.id }.toSet()
        blocks = loaded.toMap()

        val custom = customIds.size
        val override = blocks.size - custom
        logger.info("Foram carregados $custom bloco(s) custom e $override override(s) de bloco vanilla.")
    }

    private fun loadYaml(file: File, blocks: MutableMap<String, BlockDefinition>) {
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

    private fun parseOverride(key: String, section: ConfigurationSection): BlockDefinition? {
        val target = section.getString("target")?.lowercase()
        if (target == null) {
            logger.warning("Bloco '$key' (OVERRIDE) nao declara 'target'.")
            return null
        }
        if (Material.matchMaterial(target) == null) {
            logger.warning("Bloco '$key' (OVERRIDE): '$target' nao e um material valido.")
            return null
        }

        val texture = section.getString("texture")
        if (texture == null) {
            logger.warning("Bloco '$key' (OVERRIDE) nao declara 'texture'.")
            return null
        }

        if (section.contains("block_default_drop")) {
            logger.warning(
                "Bloco '$key' (OVERRIDE): 'block_default_drop' nao se aplica a overrides — " +
                    "o drop vanilla e sempre cancelado. Use 'block_drop_id'."
            )
        }

        return BlockDefinition(
            id = key,
            type = BlockEntryType.OVERRIDE,
            target = target,
            name = section.getString("name"),
            texture = texture,
            dropId = section.getString("block_drop_id"),
            dropAmount = section.getInt("block_drop_amount", 1).coerceAtLeast(1),
        )
    }

    private fun parseCustom(key: String, section: ConfigurationSection): BlockDefinition? {
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

        val combinacaoValida = fontes == listOf("texture") ||
            fontes == listOf("textures") ||
            fontes == listOf("model") ||
            fontes == listOf("textures", "model")
        if (!combinacaoValida) {
            logger.warning("Bloco '$key' (CUSTOM): aparencia ambigua (${fontes.joinToString(" + ")}).")
            return null
        }

        val itemMaterialDeclarado = section.getString("item_material", BlockDefinition.DEFAULT_ITEM_MATERIAL)
            ?.lowercase()
            ?: BlockDefinition.DEFAULT_ITEM_MATERIAL
        val material = Material.matchMaterial(itemMaterialDeclarado)
        if (material == null) {
            logger.warning("Bloco '$key' (CUSTOM): item_material '$itemMaterialDeclarado' nao e um material valido.")
            return null
        }

        val itemMaterial = if (material.isBlock) {
            itemMaterialDeclarado
        } else {
            logger.warning(
                "Bloco '$key' (CUSTOM): item_material '$itemMaterialDeclarado' nao e colocavel; " +
                    "usando ${BlockDefinition.DEFAULT_ITEM_MATERIAL}."
            )
            BlockDefinition.DEFAULT_ITEM_MATERIAL
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
            dropId = section.getString("block_drop_id"),
            dropAmount = section.getInt("block_drop_amount", 1).coerceAtLeast(1),
            defaultDrop = section.getBoolean("block_default_drop", true),
        )
    }
}
