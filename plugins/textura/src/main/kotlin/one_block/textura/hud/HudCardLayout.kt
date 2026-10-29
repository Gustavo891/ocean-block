package one_block.textura.hud

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import one_block.textura.api.TexturaTags
import one_block.textura.text.TextMetrics
import java.util.function.ToIntFunction
import kotlin.math.roundToInt

internal object HudCardLayout {

    const val GAP = 8
    const val TILE = 8
    const val MIN_BAR = 64
    const val LOWER_FONT = "minecraft:bossbar2"

    private val plainText = PlainTextComponentSerializer.plainText()

    fun compose(card: HudCard): String =
        compose(card) { text -> TextMetrics.calculateWidth(plainText.serialize(TexturaTags.miniMessage.deserialize(text))) }

    fun compose(card: HudCard, width: ToIntFunction<String>): String {
        val topLeft = width.applyAsInt(card.topLeft)
        val topRight = width.applyAsInt(card.topRight)
        val bottomRight = width.applyAsInt(card.bottomRight)
        val top = topLeft + if (card.topRight.isEmpty()) 0 else GAP + topRight
        val inner = maxOf(top, MIN_BAR + GAP + bottomRight)
        val tiles = (inner - GAP - bottomRight) / TILE
        val line1 = card.topLeft + shift(inner - topLeft - topRight) + card.topRight
        val line2 = bar(card.bar, tiles, card.progress) +
            shift(inner - tiles * TILE - bottomRight) +
            "<font:$LOWER_FONT>${card.bottomRight}</font>"
        return line1 + shift(-inner) + line2
    }

    fun filled(tiles: Int, progress: Float): Int =
        (tiles * TILE * progress.coerceIn(0f, 1f)).roundToInt()

    private fun bar(prefix: String, tiles: Int, progress: Float): String {
        val filled = filled(tiles, progress)
        return (0 until tiles).joinToString("") { index ->
            val columns = (filled - index * TILE).coerceIn(0, TILE)
            "<white><glyph:${prefix}_$columns></white><shift:-1>"
        }
    }

    private fun shift(pixels: Int): String = if (pixels == 0) "" else "<shift:$pixels>"
}
