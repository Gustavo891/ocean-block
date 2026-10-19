package one_block.textura.hud.anchor

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class AnchorPalette(val index: Int, val named: NamedTextColor) {
    BLACK(0, NamedTextColor.BLACK),
    DARK_BLUE(1, NamedTextColor.DARK_BLUE),
    DARK_GREEN(2, NamedTextColor.DARK_GREEN),
    DARK_AQUA(3, NamedTextColor.DARK_AQUA),
    DARK_RED(4, NamedTextColor.DARK_RED),
    DARK_PURPLE(5, NamedTextColor.DARK_PURPLE),
    GOLD(6, NamedTextColor.GOLD),
    GRAY(7, NamedTextColor.GRAY),
    DARK_GRAY(8, NamedTextColor.DARK_GRAY),
    BLUE(9, NamedTextColor.BLUE),
    GREEN(10, NamedTextColor.GREEN),
    AQUA(11, NamedTextColor.AQUA),
    RED(12, NamedTextColor.RED),
    LIGHT_PURPLE(13, NamedTextColor.LIGHT_PURPLE),
    YELLOW(14, NamedTextColor.YELLOW),
    WHITE(15, NamedTextColor.WHITE);

    companion object {

        fun nearest(color: TextColor?): AnchorPalette {
            if (color == null) return WHITE
            val named = NamedTextColor.nearestTo(color)
            return entries.first { it.named == named }
        }
    }
}
