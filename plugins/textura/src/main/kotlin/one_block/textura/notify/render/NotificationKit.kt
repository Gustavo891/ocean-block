package one_block.textura.notify.render

import net.kyori.adventure.key.Key
import one_block.textura.dialog.kit.UiTextFont

internal data class IconGlyph(val char: String, val advance: Int)

internal data class PlaqueGlyphs(val leftCap: String, val rightCap: String, val bodies: Map<Int, String>)

internal class NotificationKit(
    val textFont: UiTextFont,
    val textTops: Set<Int>,
    val plaque: PlaqueGlyphs?,
    val icons: Map<String, IconGlyph>,
) {

    fun icon(id: String?): IconGlyph? = id?.let { icons[NotificationIcons.normalize(it)] }

    companion object {

        fun textOnly(textFont: UiTextFont): NotificationKit = NotificationKit(textFont, emptySet(), null, emptyMap())
    }
}

internal object NotificationFonts {

    const val NAMESPACE = "textura"

    fun key(textTop: Int): Key = Key.key(NAMESPACE, "notify/$textTop")
}

internal object NotificationIcons {

    const val ITEM_PREFIX = "item:"
    const val GLYPH_PREFIX = "glyph:"

    fun normalize(id: String): String {
        val trimmed = id.trim()
        return if (trimmed.startsWith(ITEM_PREFIX, ignoreCase = true)) {
            ITEM_PREFIX + trimmed.substring(ITEM_PREFIX.length).lowercase()
        } else {
            trimmed
        }
    }

    fun item(material: String): String = ITEM_PREFIX + material.trim().lowercase()

    fun glyph(id: String): String = GLYPH_PREFIX + id
}
