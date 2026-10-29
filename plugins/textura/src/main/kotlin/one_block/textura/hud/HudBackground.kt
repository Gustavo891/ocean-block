package one_block.textura.hud

import net.kyori.adventure.text.Component
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette
import org.bukkit.Bukkit
import one_block.textura.api.GlyphApi
import one_block.textura.text.TextMetrics
import java.util.concurrent.ConcurrentHashMap

object HudBackground {

    const val DEFAULT_GLYPH_WIDTH = 6
    const val DEFAULT_PREFIX = "hud"

    private val warned = ConcurrentHashMap.newKeySet<String>()

    data class Glyphs(
        val left: String, val body: String, val right: String,
        val leftW: Int, val bodyW: Int, val rightW: Int,
    ) {
        val available: Boolean get() = left.isNotEmpty() && body.isNotEmpty() && right.isNotEmpty()
    }

    fun resolve(): Glyphs = resolve(DEFAULT_PREFIX)

    fun resolve(prefix: String): Glyphs {
        val ids = listOf("${prefix}_left", "${prefix}_body", "${prefix}_right")
        val g = Glyphs(
            left = GlyphApi.charOrEmpty(ids[0]),
            body = GlyphApi.charOrEmpty(ids[1]),
            right = GlyphApi.charOrEmpty(ids[2]),
            leftW = width(ids[0]),
            bodyW = width(ids[1]),
            rightW = width(ids[2]),
        )
        if (!g.available && prefix != DEFAULT_PREFIX) return resolve(DEFAULT_PREFIX)
        if (!g.available && warned.add(prefix)) {
            Bukkit.getLogger().warning(
                "[ResourcePack] Glyphs da HUD (${ids.joinToString("/")}) não registrados; " +
                    "HUDs serão renderizadas apenas com texto."
            )
        }
        return g
    }

    fun textStart(width: Int, textPixels: Int, g: Glyphs): Int =
        g.leftW + (width - g.leftW - g.rightW - textPixels) / 2

    private fun width(id: String): Int = GlyphApi.getSize(id)?.takeIf { it > 0 } ?: DEFAULT_GLYPH_WIDTH
    fun build(segments: Int, g: Glyphs): Component {
        var comp: Component = Component.empty()
        for (i in 0 until segments) {
            val ch = when (i) {
                0 -> g.left
                segments - 1 -> g.right
                else -> g.body
            }
            comp = comp.append(Component.text(ch, AnchorMarker.raisedGlyph(AnchorPalette.WHITE)).shadowColor(AnchorMarker.raisedShadow(AnchorPalette.GRAY, 30)))
            if (i < segments - 1) comp = comp.append(TextMetrics.getSpacingComponent(-1))
        }
        return comp
    }
}
