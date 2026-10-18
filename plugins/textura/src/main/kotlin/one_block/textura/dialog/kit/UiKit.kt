package one_block.textura.dialog.kit

import net.kyori.adventure.key.Key

data class UiPiece(val char: String, val advance: Int)

data class UiSlice(
    val left: Int,
    val right: Int,
    val leftCap: UiPiece?,
    val rightCap: UiPiece?,
    val middles: Map<Int, UiPiece>,
)

data class UiImage(val char: String, val advance: Int, val width: Int, val height: Int, val slice: UiSlice? = null)

data class SurfaceSize(val id: String, val width: Int, val height: Int)

data class SurfaceHeight(val id: String, val height: Int)

data class IconSize(val id: String, val size: Int)

interface UiKit {
    fun has(id: String): Boolean
    fun image(id: String): UiImage?
    fun surface(size: SurfaceSize): UiImage?
    fun icon(size: IconSize): UiImage?
    fun imageFont(offset: Int): Key
    fun textFont(offset: Int): Key
    val spaceFont: Key
    fun spaceChar(amount: Int): String?
    fun charAdvance(codePoint: Int): Int
    val hit: UiPiece?
}

object UiFonts {

    const val VARIANTS = 9
    const val BASE_ASCENT = 7
    private const val NAMESPACE = "oneblock"
    private val SPACE_STEPS = (0..8).map { 1 shl it }

    val SPACE: Key = Key.key(NAMESPACE, "ui/space")

    fun image(offset: Int): Key = Key.key(NAMESPACE, "ui/image_$offset")

    fun text(offset: Int): Key = Key.key(NAMESPACE, "ui/text_$offset")

    fun spaceChars(): Map<Int, String> = buildMap {
        SPACE_STEPS.forEachIndexed { index, step ->
            put(step, String(Character.toChars(0xE000 + index)))
            put(-step, String(Character.toChars(0xE000 + SPACE_STEPS.size + index)))
        }
    }
}
