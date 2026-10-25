package one_block.textura.dialog.kit

import net.kyori.adventure.key.Key

class PackedUiKit(
    private val images: Map<String, UiImage>,
    private val surfaces: Map<SurfaceHeight, UiImage>,
    private val icons: Map<IconSize, UiImage>,
    private val textFont: UiTextFont,
    override val hit: UiPiece?,
    private val pixels: Map<String, UiPiece> = emptyMap(),
) : UiKit {

    override fun pixel(scale: Int, width: Int): UiPiece? = pixels[UiPixels.name(scale, width)]

    private val spaces = UiFonts.spaceChars()
    private val known: Set<String> = images.keys + icons.keys.map { it.id } + surfaces.keys.map { it.id }

    override fun has(id: String): Boolean = id in known

    override fun image(id: String): UiImage? = images[id]

    override fun surface(size: SurfaceSize): UiImage? =
        surfaces[SurfaceHeight(size.id, size.height)]?.copy(width = size.width)

    override fun icon(size: IconSize): UiImage? = icons[size]

    override fun imageFont(offset: Int): Key = UiFonts.image(offset)

    override fun textFont(offset: Int): Key = UiFonts.text(offset)

    override val spaceFont: Key = UiFonts.SPACE

    override fun spaceChar(amount: Int): String? = spaces[amount]

    override fun charAdvance(codePoint: Int): Int = textFont.advance(codePoint)
}
