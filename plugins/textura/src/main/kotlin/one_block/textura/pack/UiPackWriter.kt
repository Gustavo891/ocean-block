package one_block.textura.pack

import net.kyori.adventure.key.Key
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.GlyphAdvance
import one_block.textura.dialog.kit.IconScale
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.PackedUiKit
import one_block.textura.dialog.kit.SurfaceComposer
import one_block.textura.dialog.kit.SurfaceHeight
import one_block.textura.dialog.kit.UiArt
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiCodepoints
import one_block.textura.dialog.kit.UiFonts
import one_block.textura.dialog.kit.UiImage
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiPiece
import one_block.textura.dialog.kit.UiPieceSource
import one_block.textura.dialog.kit.UiPieces
import one_block.textura.dialog.kit.UiPixels
import one_block.textura.dialog.kit.UiSlice
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.dialog.theme.UiTheme
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.texture.Texture
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.logging.Logger
import javax.imageio.ImageIO

class UiPackWriter(
    private val catalog: UiArtCatalog,
    private val textFont: UiTextFont,
    private val screens: Collection<ScreenDefinition>,
    private val themes: Map<String, UiTheme>,
    private val logger: Logger,
    private val shader: Boolean,
) {

    private data class Packed(val texture: Key, val height: Int, val piece: UiPiece)

    fun write(builder: PackBuilder): UiKit {
        val warned = mutableSetOf<String>()
        val usage = UiUsage.collect(screens, themes, BaseUiKit(catalog, textFont)) { if (warned.add(it)) logger.warning("[ui] $it") }

        val arts = usage.images.mapNotNull { id -> catalog.art(id) ?: missing(id) }.associateBy { it.id }
        val surfaces = usage.surfaces.mapNotNull { size -> surface(size)?.let { size to it } }.toMap()
        val icons = usage.icons.mapNotNull { size -> icon(size)?.let { size to it } }.toMap()
        val hit = UiPieceSource(HIT, BufferedImage(1, HIT_HEIGHT, BufferedImage.TYPE_INT_ARGB))

        val pixels = pixelSources()
        val sources = arts.values.flatMap(::artSources) + surfaces.values.flatMap { it.pieces } + icons.values + hit + pixels
        val chars = UiCodepoints.assign(sources.map { it.name })
        val packed = sources.associate { it.name to texture(builder, it, chars.getValue(it.name)) }

        writeFonts(builder, packed.values)
        if (shader) writeShaders(builder)
        logger.info("[ui] ${sources.size} peça(s), ${surfaces.size} surface(s), ${icons.size} ícone(s), ${screens.size} tela(s).")

        return PackedUiKit(
            images = arts.mapValues { (id, art) -> image(id, art, packed) },
            surfaces = surfaces.mapValues { (size, surface) -> surfaceImage(surface, size.height, packed) },
            icons = icons.mapValues { (size, source) -> plain(packed.getValue(source.name), source.image.width / source.detail, size.size) },
            textFont = textFont,
            hit = packed.getValue(HIT).piece,
            pixels = pixels.associate { it.name to packed.getValue(it.name).piece },
        )
    }

    private fun pixelSources(): List<UiPieceSource> = UiPixels.SCALES.flatMap { scale ->
        UiPixels.WIDTHS.map { width ->
            val image = BufferedImage(width * scale, scale, BufferedImage.TYPE_INT_ARGB)
            for (x in 0 until image.width) for (y in 0 until image.height) image.setRGB(x, y, WHITE)
            UiPieceSource(UiPixels.name(scale, width), image)
        }
    } + UiPixels.DETAILS.flatMap { detail ->
        UiPixels.WIDTHS.flatMap { width ->
            (0 until detail).flatMap { dx ->
                (0 until detail).map { dy ->
                    val image = BufferedImage(dx + width, UiFonts.BASE_ASCENT.inc() * detail, BufferedImage.TYPE_INT_ARGB)
                    for (x in dx until dx + width) image.setRGB(x, dy, WHITE)
                    UiPieceSource(UiPixels.detailName(detail, width, dx, dy), image, detail)
                }
            }
        }
    }

    private fun plain(packed: Packed, width: Int, height: Int): UiImage =
        UiImage(packed.piece.char, packed.piece.advance, width, height)

    private fun image(id: String, art: UiArt, packed: Map<String, Packed>): UiImage {
        val main = packed.getValue(id).piece
        val slice = art.slice?.takeUnless { it.nine }?.let { s ->
            UiSlice(
                left = s.left,
                right = s.right,
                leftCap = packed["$id#left"]?.piece,
                rightCap = packed["$id#right"]?.piece,
                middles = UiPieces.MIDDLE_SIZES.associateWith { packed.getValue("$id#mid$it").piece },
            )
        }
        return UiImage(main.char, main.advance, art.image.width, art.image.height, slice)
    }

    private fun artSources(art: UiArt): List<UiPieceSource> {
        val main = UiPieceSource(art.id, art.image)
        val slice = art.slice?.takeUnless { it.nine } ?: return listOf(main)
        return listOf(main) + UiPieces.slices(art.id, art.image, slice.left, slice.right)
    }

    private class SurfacePieces(val name: String, val left: Int, val right: Int, val pieces: List<UiPieceSource>)

    private fun surface(size: SurfaceHeight): SurfacePieces? {
        val art = catalog.art(size.id) ?: return missing(size.id)
        val slice = art.slice?.takeIf { it.nine } ?: run {
            logger.warning("[ui] Surface '${size.id}' precisa de .slice com 'esquerda direita topo base'")
            return null
        }
        if (size.height > UiArtCatalog.MAX_HEIGHT) {
            logger.warning("[ui] Surface '${size.id}' com ${size.height} px de altura passa de ${UiArtCatalog.MAX_HEIGHT}; fundo não será desenhado")
            return null
        }
        val middle = art.image.width - slice.left - slice.right
        if (middle <= 0) {
            logger.warning("[ui] Surface '${size.id}' precisa de ao menos 1 px de meio entre as tampas esquerda e direita")
            return null
        }
        val strip = SurfaceComposer.compose(art.image, slice, art.image.width, size.height) ?: run {
            logger.warning("[ui] Surface '${size.id}' com ${size.height} px é menor que as tampas")
            return null
        }
        val name = "surface:${size.id}@${size.height}"
        val pieces = UiPieces.slices(name, strip, slice.left, slice.right)
        return SurfacePieces(name, slice.left, slice.right, pieces)
    }

    private fun surfaceImage(surface: SurfacePieces, height: Int, packed: Map<String, Packed>): UiImage {
        val slice = UiSlice(
            left = surface.left,
            right = surface.right,
            leftCap = packed["${surface.name}#left"]?.piece,
            rightCap = packed["${surface.name}#right"]?.piece,
            middles = UiPieces.MIDDLE_SIZES.mapNotNull { size -> packed["${surface.name}#mid$size"]?.let { size to it.piece } }.toMap(),
        )
        return UiImage("", 0, 0, height, slice)
    }

    private fun icon(size: IconSize): UiPieceSource? {
        val art = catalog.art(size.id) ?: return missing(size.id)
        val native = if (size.id.startsWith(UiArtCatalog.ITEM_PREFIX) && size.size < art.image.height) IconScale.native(art.image, size.size) else null
        val detail = if (native == null) IconScale.detail(art.image, size.size) else 1
        val resized = native ?: IconScale.resize(art.image, size.size * detail) ?: run {
            logger.warning("[ui] Ícone '${size.id}' com tamanho inválido ${size.size}")
            return null
        }
        return UiPieceSource("icon:${size.id}@${size.size}", resized, detail)
    }

    private fun <T> missing(id: String): T? {
        logger.warning("[ui] Imagem '$id' não encontrada em resource/assets/oneblock/textures/ui/ nem nos glyphs nem no client vanilla")
        return null
    }

    private fun texture(builder: PackBuilder, source: UiPieceSource, char: String): Packed {
        val padded = UiPieces.padded(source.image)
        val key = Key.key(NAMESPACE, "ui_gen/${hash(source.name)}.png")
        builder.pack.texture(Texture.texture(key, Writable.bytes(png(padded))))
        return Packed(key, padded.height / source.detail, UiPiece(char, GlyphAdvance.of(source.image, source.detail)))
    }

    private fun writeFonts(builder: PackBuilder, pieces: Collection<Packed>) {
        val textSpace = FontProvider.space(mapOf(" " to UiTextFont.SPACE_ADVANCE, Char(0x200C).toString() to 0))

        for (k in 0 until UiFonts.VARIANTS) {
            builder.addFont(
                UiFonts.image(k),
                pieces.map { FontProvider.bitMap(it.texture, it.height, UiFonts.BASE_ASCENT - k, listOf(it.piece.char)) },
            )
            builder.addFont(
                UiFonts.text(k),
                listOf<FontProvider>(textSpace) + textFont.providers.map { FontProvider.bitMap(it.file, it.height, it.ascent - k, it.chars) },
            )
            textFont.extras.forEach { (name, extra) ->
                builder.addFont(
                    UiFonts.text(k, name),
                    listOf<FontProvider>(textSpace) + extra.providers.map { FontProvider.bitMap(it.file, it.height, it.ascent - k, it.chars) },
                )
            }
        }
        builder.addFont(UiFonts.SPACE, listOf(FontProvider.space(UiFonts.spaceChars().entries.associate { (amount, char) -> char to amount })))
    }

    private fun writeShaders(builder: PackBuilder) {
        SHADERS.forEach { name ->
            builder.pack.unknownFile("assets/minecraft/shaders/core/$name", Writable.resource(javaClass.classLoader, "ui/$name"))
        }
    }

    private fun png(image: BufferedImage): ByteArray =
        ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }

    private fun hash(name: String): String =
        MessageDigest.getInstance("SHA-1").digest(name.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16)

    companion object {
        private const val NAMESPACE = "oneblock"
        private const val HIT = "ui:#hit"
        private const val HIT_HEIGHT = 9
        private const val WHITE = 0xFFFFFFFF.toInt()
        private val SHADERS = listOf("gui.vsh", "gui.fsh")
    }
}
