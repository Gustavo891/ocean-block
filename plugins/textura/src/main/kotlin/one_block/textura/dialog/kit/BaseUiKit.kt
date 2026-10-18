package one_block.textura.dialog.kit

import net.kyori.adventure.key.Key

class BaseUiKit(private val catalog: UiArtCatalog, private val textFont: UiTextFont) : UiKit {

    override fun has(id: String): Boolean = catalog.art(id) != null

    override fun image(id: String): UiImage? = catalog.art(id)?.let { art ->
        val slice = art.slice?.takeUnless { it.nine }?.let { UiSlice(it.left, it.right, null, null, emptyMap()) }
        UiImage("", GlyphAdvance.of(art.image), art.image.width, art.image.height, slice)
    }

    override fun surface(size: SurfaceSize): UiImage? = null

    override fun icon(size: IconSize): UiImage? = null

    override fun imageFont(offset: Int): Key = UiFonts.image(offset)

    override fun textFont(offset: Int): Key = UiFonts.text(offset)

    override val spaceFont: Key = UiFonts.SPACE

    override fun spaceChar(amount: Int): String? = null

    override fun charAdvance(codePoint: Int): Int = textFont.advance(codePoint)

    override val hit: UiPiece? = null
}
