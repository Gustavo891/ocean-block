package one_block.textura.hud

import net.kyori.adventure.text.Component
import one_block.textura.hud.anchor.AnchorMarker
import one_block.textura.hud.anchor.AnchorPalette
import org.bukkit.Bukkit
import one_block.textura.api.GlyphApi
import one_block.textura.text.TextMetrics

object HudBackground {

    const val DEFAULT_GLYPH_WIDTH = 6

    private const val ID_LEFT = "hud_left"
    private const val ID_BODY = "hud_body"
    private const val ID_RIGHT = "hud_right"

    @Volatile
    private var warned = false

    data class Glyphs(
        val left: String, val body: String, val right: String,
        val leftW: Int, val bodyW: Int, val rightW: Int,
    ) {
        val available: Boolean get() = left.isNotEmpty() && body.isNotEmpty() && right.isNotEmpty()
    }

    fun resolve(): Glyphs {
        val g = Glyphs(
            left = GlyphApi.charOrEmpty(ID_LEFT),
            body = GlyphApi.charOrEmpty(ID_BODY),
            right = GlyphApi.charOrEmpty(ID_RIGHT),
            leftW = DEFAULT_GLYPH_WIDTH,
            bodyW = DEFAULT_GLYPH_WIDTH,
            rightW = DEFAULT_GLYPH_WIDTH,
        )
        if (!g.available && !warned) {
            warned = true
            Bukkit.getLogger().warning(
                "[ResourcePack] Glyphs da HUD ($ID_LEFT/$ID_BODY/$ID_RIGHT) não registrados; " +
                    "HUDs serão renderizadas apenas com texto."
            )
        }
        return g
    }

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
