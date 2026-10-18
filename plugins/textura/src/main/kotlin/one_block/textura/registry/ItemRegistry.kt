package one_block.textura.registry

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import one_block.textura.model.DisplayTransform
import one_block.textura.model.ItemData
import one_block.textura.model.ItemType
import one_block.textura.model.Vec3
import java.io.File
import java.util.logging.Logger

class ItemRegistry(private val logger: Logger) {

    @Volatile
    var items: Map<String, ItemData> = emptyMap()
        private set

    fun load(itemsFolder: File) {
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs()
            items = emptyMap()
            return
        }

        val loaded = linkedMapOf<String, ItemData>()
        YamlFiles.collect(itemsFolder).forEach { loadYaml(it, loaded) }
        items = loaded.toMap()

        logger.info("Foram carregados ${items.size} itens customizados.")
    }

    private fun loadYaml(file: File, items: MutableMap<String, ItemData>) {
        val config = YamlConfiguration.loadConfiguration(file)
        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key) ?: continue
            val typeStr = section.getString("type", "FLAT")?.uppercase() ?: "FLAT"
            val type = try {
                ItemType.valueOf(typeStr)
            } catch (e: Exception) {
                logger.warning("Tipo de item invalido: $typeStr para o item $key")
                continue
            }
            
            val material = section.getString("material")
            if (material == null) {
                logger.warning("O item $key nao possui um material definido.")
                continue
            }
            
            val customModelData = section.getInt("custom_model_data", -1)
            val itemModel = section.getString("item_model")
            if (customModelData == -1 && itemModel == null) {
                logger.warning("O item $key nao possui custom_model_data nem item_model definido.")
                continue
            }

            val texturePath = section.getString("texture")
            val modelPath = section.getString("model")
            val textureKey = section.getString("texture_key")?.takeIf { it.isNotBlank() }
            val oversizedInGui =
                if (section.contains("oversized_in_gui")) section.getBoolean("oversized_in_gui") else null

            if (items.containsKey(key)) {
                logger.warning("Item '$key' duplicado em ${file.name}; a segunda definicao foi ignorada.")
                continue
            }
            items[key] = ItemData(
                id = key,
                type = type,
                material = material.lowercase(),
                customModelData = customModelData,
                texturePath = texturePath,
                modelPath = modelPath,
                itemModel = itemModel,
                textureKey = textureKey,
                oversizedInGui = oversizedInGui,
                name = section.getString("name"),
                lore = section.getStringList("lore"),
                display = parseDisplay(key, section)
            )
        }
    }

    /**
     * Lê o `display:` completo (uma seção por perspectiva) e o atalho `scale:`,
     * que vale só para o `gui` — o caso comum de "quero maior no inventário".
     * Um `display.gui.scale` explícito tem prioridade sobre o atalho.
     */
    private fun parseDisplay(id: String, section: ConfigurationSection): Map<String, DisplayTransform> {
        val result = linkedMapOf<String, DisplayTransform>()

        section.getConfigurationSection("display")?.let { display ->
            for (perspective in display.getKeys(false)) {
                val entry = display.getConfigurationSection(perspective)
                if (entry == null) {
                    logger.warning("Item $id: display.$perspective nao e uma secao; ignorado.")
                    continue
                }

                val transform = DisplayTransform(
                    rotation = readVec(id, entry, "rotation"),
                    translation = readVec(id, entry, "translation"),
                    scale = readVec(id, entry, "scale"),
                )
                if (transform.isEmpty) continue
                result[perspective.lowercase()] = transform
            }
        }

        readVec(id, section, "scale")?.let { scale ->
            val gui = result["gui"]
            result["gui"] = gui?.copy(scale = gui.scale ?: scale) ?: DisplayTransform(scale = scale)
        }

        return result
    }

    /**
     * Aceita as três formas: escalar (`scale: 2`), mapa (`x/y/z`) e lista
     * (`[2, 2, 2]`, o formato do model JSON do próprio jogo).
     */
    private fun readVec(id: String, section: ConfigurationSection, path: String): Vec3? {
        if (!section.contains(path)) return null

        section.getConfigurationSection(path)?.let {
            return Vec3(
                it.getDouble("x").toFloat(),
                it.getDouble("y").toFloat(),
                it.getDouble("z").toFloat(),
            )
        }

        val list = section.getList(path)
        if (list != null) {
            val numbers = list.mapNotNull { (it as? Number)?.toFloat() }
            if (numbers.size >= 3) return Vec3(numbers[0], numbers[1], numbers[2])
            logger.warning("Item $id: '$path' precisa de 3 numeros; ignorado.")
            return null
        }

        val scalar = section.get(path) as? Number
        if (scalar == null) {
            logger.warning("Item $id: '$path' invalido; use um numero, uma lista [x, y, z] ou x/y/z.")
            return null
        }
        return Vec3(scalar.toFloat(), scalar.toFloat(), scalar.toFloat())
    }
}
