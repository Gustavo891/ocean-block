package one_block.textura.pack

import net.kyori.adventure.key.Key
import one_block.textura.model.ItemData
import one_block.textura.model.ItemType
import one_block.textura.model.Vec3
import one_block.textura.registry.ItemRegistry
import team.unnamed.creative.base.Vector3Float
import team.unnamed.creative.base.Writable
import team.unnamed.creative.model.ItemTransform
import team.unnamed.creative.model.Model
import team.unnamed.creative.model.ModelTexture
import team.unnamed.creative.model.ModelTextures
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Grava os itens no pack: a definição por material (`range_dispatch` de
 * custom_model_data), as definições por `item_model` e os modelos/texturas de
 * cada item.
 */
class ItemPackWriter(
    private val itemRegistry: ItemRegistry,
    private val assets: PackAssets,
    private val logger: Logger,
) {

    fun write(builder: PackBuilder) {
        val itemsFolder = assets.configFolder("items")
        if (!itemsFolder.exists()) itemsFolder.mkdirs()

        writeItemModelDefinitions(builder, itemsFolder)

        // Itens sem custom_model_data vivem só pelo item_model e não entram no
        // range_dispatch do material.
        val byMaterial = itemRegistry.items.values
            .filter { it.hasCustomModelData }
            .groupBy { it.material }

        for ((material, itemsList) in byMaterial) isolated("material $material") {
            val entries = mutableListOf<String>()

            // range_dispatch exige que os thresholds estejam ordenados
            val sortedItems = itemsList.sortedBy { it.customModelData }

            for (item in sortedItems) {
                val modelKeyStr = writeItemModel(builder, itemsFolder, item) ?: continue

                entries.add("""
                {
                  "threshold": ${item.customModelData.toFloat()},
                  "model": ${modelBlockJson(item.material, modelKeyStr).replace("\n", "\n                  ")}
                }
                """.trimIndent())
            }

            val fallbackJson = vanillaFallback(material)

            // O arquivo do material e um so, entao basta um item do grupo pedir.
            val oversizedInGui =
                if (sortedItems.any { it.isOversizedInGui }) ",\n  \"oversized_in_gui\": true" else ""

            val itemDefinitionJson = """
            {
              "model": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:custom_model_data",
                "entries": [
                  ${entries.joinToString(",\n                  ").replace("\n", "\n                  ")}
                ],
                "fallback": $fallbackJson
              }$oversizedInGui
            }
            """.trimIndent()

            builder.pack.unknownFile("assets/minecraft/items/$material.json", Writable.stringUtf8(itemDefinitionJson))
        }
    }

    /**
     * Gera um `assets/<namespace>/items/<caminho>.json` para cada item que declara
     * `item_model`. O cliente resolve o modelo pela chave do componente
     * `minecraft:item_model`, sem depender de custom_model_data nem do material.
     */
    private fun writeItemModelDefinitions(builder: PackBuilder, itemsFolder: File) {
        for (item in itemRegistry.items.values) isolated(item.id) {
            val namespace = item.itemModelNamespace ?: continue
            val path = item.itemModelPath ?: continue

            val modelKeyStr = writeItemModel(builder, itemsFolder, item) ?: continue

            val oversized = if (item.isOversizedInGui) ",\n  \"oversized_in_gui\": true" else ""

            val definition = """
            {
              "model": ${modelBlockJson(item.material, modelKeyStr).replace("\n", "\n              ")}$oversized
            }
            """.trimIndent()

            builder.pack.unknownFile("assets/$namespace/items/$path.json", Writable.stringUtf8(definition))
        }
    }

    /**
     * Escreve a textura (FLAT) ou o modelo (MODEL) do item no pack e devolve a
     * chave do modelo a referenciar, ou `null` quando o arquivo de origem falta.
     */
    private fun writeItemModel(builder: PackBuilder, itemsFolder: File, item: ItemData): String? {
        if (item.type == ItemType.FLAT) {
            val textureKey = writeTexture(builder, itemsFolder, item) ?: return null

            val flatModel = Model.model()
                .key(Key.key(MODEL_NAMESPACE, "item/${item.id}"))
                .parent(Key.key("minecraft", "item/generated"))
                .textures(ModelTextures.builder().layers(listOf(ModelTexture.ofKey(textureKey))).build())
                .display(displayOf(item))
                .build()

            builder.pack.model(flatModel)
            return flatModel.key().asString()
        } else if (item.type == ItemType.MODEL) {
            val modelPath = item.modelPath ?: return null

            // Um modelo customizado costuma apontar para uma textura em namespace
            // proprio; se o item declarou uma, ela e gravada nessa chave.
            if (item.texturePath != null) writeTexture(builder, itemsFolder, item)

            if (!modelPath.endsWith(".json")) {
                // Chave direta do jogo, ex.: minecraft:block/acacia_fence
                return withDisplay(builder, item, modelPath)
            }

            val asset = assets.resolve(
                declared = modelPath,
                legacyRoot = itemsFolder,
                legacyPackPath = "assets/minecraft/models/item/${item.id}.json"
            )

            if (asset == null) {
                logger.info("Modelo ${item.id} (caminho $modelPath) nao encontrado. Assumindo que esta provido em resource/ ou external/")
                return withDisplay(builder, item, "minecraft:item/${item.id}")
            }

            if (!asset.inResource) builder.pack.unknownFile(asset.packPath, Writable.file(asset.file))
            return withDisplay(builder, item, asset.key.asString())
        }

        return "minecraft:item/${item.id}"
    }

    /**
     * Para itens `model`, o JSON do modelo é copiado como está — o `display` do
     * YAML entra por um modelo-filho que herda [parentKey] e sobrescreve só as
     * perspectivas declaradas. Sem `display`, devolve a própria chave do pai.
     */
    private fun withDisplay(builder: PackBuilder, item: ItemData, parentKey: String): String {
        val display = displayOf(item)
        if (display.isEmpty()) return parentKey

        val key = Key.key(MODEL_NAMESPACE, "item/${item.id}_display")
        builder.pack.model(
            Model.model()
                .key(key)
                .parent(Key.key(parentKey))
                .display(display)
                .build()
        )
        return key.asString()
    }

    /**
     * Converte o `display` do YAML para a API do Creative. Perspectiva
     * desconhecida vira aviso e é descartada; valores fora do range aceito pelo
     * jogo são grampeados para não derrubar a montagem do pack.
     */
    private fun displayOf(item: ItemData): Map<ItemTransform.Type, ItemTransform> {
        if (item.display.isEmpty()) return emptyMap()

        val result = linkedMapOf<ItemTransform.Type, ItemTransform>()
        for ((perspective, transform) in item.display) {
            val type = runCatching { ItemTransform.Type.valueOf(perspective.uppercase()) }.getOrNull()
            if (type == null) {
                logger.warning("Item ${item.id}: perspectiva de display desconhecida: $perspective")
                continue
            }

            result[type] = ItemTransform.builder()
                .rotation(vectorOf(transform.rotation, ItemTransform.DEFAULT_ROTATION, -360f, 360f))
                .translation(vectorOf(transform.translation, ItemTransform.DEFAULT_TRANSLATION, ItemTransform.MIN_TRANSLATION, ItemTransform.MAX_TRANSLATION))
                .scale(vectorOf(transform.scale, ItemTransform.DEFAULT_SCALE, 0f, ItemTransform.MAX_SCALE))
                .build()
        }
        return result
    }

    private fun vectorOf(value: Vec3?, default: Vector3Float, min: Float, max: Float): Vector3Float {
        if (value == null) return default
        return Vector3Float(
            value.x.coerceIn(min, max),
            value.y.coerceIn(min, max),
            value.z.coerceIn(min, max),
        )
    }

    /**
     * Grava o PNG do item no pack e devolve a chave da textura: a de `texture_key`
     * quando declarada, senão `minecraft:item/<id>`. `null` se o arquivo não existe.
     */
    private fun writeTexture(builder: PackBuilder, itemsFolder: File, item: ItemData): Key? {
        val texturePath = item.texturePath ?: return null

        val legacyPackPath = item.textureKey
            ?.let { "assets/${item.textureNamespace}/textures/${item.texturePathInPack}.png" }
            ?: "assets/minecraft/textures/item/${item.id}.png"

        val asset = assets.resolve(texturePath, itemsFolder, legacyPackPath)
        if (asset == null) {
            logger.warning("Textura do item ${item.id} nao encontrada: $texturePath")
            return null
        }

        if (!asset.inResource) builder.pack.unknownFile(asset.packPath, Writable.file(asset.file))

        // O atlas 'blocks' do vanilla so varre textures/item e textures/block. Uma
        // textura organizada em qualquer outra pasta existe no pack mas nunca e
        // costurada, e o item sai com o xadrez roxo. Quando a chave e nossa (item
        // sem texture_key proprio), uma copia entra em textures/item para o atlas
        // achar, sem obrigar ninguem a achatar a organizacao de resource/.
        if (item.textureKey == null && !dentroDoAtlas(asset.packPath)) {
            val destino = "assets/${namespaceDe(asset.packPath)}/textures/item/${item.id}.png"
            if (destino != asset.packPath) {
                builder.pack.unknownFile(destino, Writable.file(asset.file))
                return PackAssets.keyOf(destino, keepExtension = false)
            }
        }

        return asset.key
    }

    private fun dentroDoAtlas(packPath: String): Boolean =
        packPath.contains("/textures/item/") || packPath.contains("/textures/block/")

    private fun namespaceDe(packPath: String): String {
        val partes = packPath.split('/')
        return if (partes.size >= 2 && partes[0] == "assets") partes[1] else "minecraft"
    }

    private fun modelBlockJson(material: String, modelKeyStr: String): String =
        if (material == "player_head") {
            """
            {
              "type": "minecraft:special",
              "model": {
                "type": "minecraft:player_head"
              },
              "base": "$modelKeyStr"
            }
            """.trimIndent()
        } else {
            """
            {
              "type": "minecraft:model",
              "model": "$modelKeyStr"
            }
            """.trimIndent()
        }

    private fun vanillaFallback(material: String): String {
        return when (material) {
            "player_head" -> """
                {
                  "type": "minecraft:special",
                  "model": {
                    "type": "minecraft:player_head"
                  },
                  "base": "minecraft:item/template_skull"
                }
            """.trimIndent().replace("\n", "\n                ")
            "shield" -> """
                {
                  "type": "minecraft:condition",
                  "property": "minecraft:using_item",
                  "on_true": {
                    "type": "minecraft:special",
                    "base": "minecraft:item/shield_blocking",
                    "model": {
                      "type": "minecraft:shield"
                    }
                  },
                  "on_false": {
                    "type": "minecraft:special",
                    "base": "minecraft:item/shield",
                    "model": {
                      "type": "minecraft:shield"
                    }
                  }
                }
            """.trimIndent().replace("\n", "\n                ")
            "trident" -> """
                {
                  "type": "minecraft:condition",
                  "property": "minecraft:using_item",
                  "on_true": {
                    "type": "minecraft:special",
                    "base": "minecraft:item/trident_in_hand",
                    "model": {
                      "type": "minecraft:trident"
                    }
                  },
                  "on_false": {
                    "type": "minecraft:model",
                    "model": "minecraft:item/trident"
                  }
                }
            """.trimIndent().replace("\n", "\n                ")
            else -> """
                {
                  "type": "minecraft:model",
                  "model": "minecraft:item/$material"
                }
            """.trimIndent().replace("\n", "\n                ")
        }
    }

    private inline fun isolated(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Item '$what' ignorado no pack", e)
        }
    }

    private companion object {
        const val MODEL_NAMESPACE = "oneblock"
    }
}
