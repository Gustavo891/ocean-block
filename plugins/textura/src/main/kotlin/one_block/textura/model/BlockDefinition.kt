package one_block.textura.model

enum class BlockEntryType { OVERRIDE, CUSTOM }

enum class BlockBase { NOTE_BLOCK }

data class BlockDefinition(
    val id: String,
    val type: BlockEntryType,
    val target: String? = null,
    val name: String? = null,
    val base: BlockBase = BlockBase.NOTE_BLOCK,
    val texture: String? = null,
    val textures: Map<String, String> = emptyMap(),
    val model: String? = null,
    val itemMaterial: String = DEFAULT_ITEM_MATERIAL,
    val dropId: String? = null,
    val dropAmount: Int = 1,
    val defaultDrop: Boolean = true,
) {
    val translationKey: String
        get() = when (type) {
            BlockEntryType.OVERRIDE -> "block.minecraft.$target"
            BlockEntryType.CUSTOM -> "block.$NAMESPACE.$id"
        }

    val itemModelKey: String get() = "$NAMESPACE:$id"

    val blockModelKey: String get() = "$NAMESPACE:block/$id"

    val itemModelPath: String get() = "$NAMESPACE:item/$id"

    companion object {
        const val NAMESPACE = "oneblock"
        const val DEFAULT_ITEM_MATERIAL = "note_block"
    }
}
