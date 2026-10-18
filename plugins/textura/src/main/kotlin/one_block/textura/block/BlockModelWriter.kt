package one_block.textura.block

import one_block.textura.model.BlockDefinition

object BlockModelWriter {

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

    fun itemModelJson(definition: BlockDefinition): String = """
    {
      "parent": "${definition.blockModelKey}"
    }
    """.trimIndent()

    fun itemDefinitionJson(definition: BlockDefinition): String = """
    {
      "model": {
        "type": "minecraft:model",
        "model": "${definition.itemModelPath}"
      }
    }
    """.trimIndent()

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
