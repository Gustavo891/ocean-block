package one_block.textura.pack

import net.kyori.adventure.key.Key
import one_block.textura.dialog.kit.GlyphAdvance
import one_block.textura.dialog.kit.IconScale
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.notify.layout.LineGeometry
import one_block.textura.notify.layout.PlaquePieces
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.notify.render.BackdropGlyph
import one_block.textura.notify.render.IconGlyph
import one_block.textura.notify.render.NotificationFonts
import one_block.textura.notify.render.NotificationIcons
import one_block.textura.notify.render.NotificationKit
import one_block.textura.notify.render.PlaqueGlyphs
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.texture.Texture
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.logging.Logger
import javax.imageio.ImageIO

internal class NotificationPackWriter(
    private val regions: List<NotificationRegion>,
    private val textFont: UiTextFont,
    private val materials: Set<String>,
    private val itemTexture: (String) -> BufferedImage?,
    private val glyphFiles: Map<String, File>,
    private val plaqueSource: File?,
    private val logger: Logger,
) {

    private class Baked(val texture: Key, val height: Int, val ascentOffset: Int, val char: String)

    fun write(builder: PackBuilder): NotificationKit {
        val tops = LineGeometry.textTops(regions)
        val codepoints = generateSequence(FIRST_CODEPOINT) { it + 1 }.iterator()
        fun nextChar(): String = String(Character.toChars(codepoints.next()))

        val color = plaqueColor()
        val left = bake(builder, "plaque_left", cap(color), nextChar(), PLAQUE_OFFSET)
        val right = bake(builder, "plaque_right", cap(color), nextChar(), PLAQUE_OFFSET)
        val bodies = PlaquePieces.BODY_WIDTHS.associateWith { width -> bake(builder, "plaque_body_$width", fill(width, color), nextChar(), PLAQUE_OFFSET) }

        val icons = linkedMapOf<String, Pair<Baked, Int>>()
        materials.forEach { material ->
            val image = itemTexture(material)?.let(::scaled)
            if (image == null) {
                logger.warning("[notificacoes] Ícone item:$material sem textura vanilla; fica de fora")
                return@forEach
            }
            icons[NotificationIcons.item(material)] = bake(builder, "item/$material", image, nextChar(), ICON_OFFSET) to GlyphAdvance.of(image)
        }
        val backdrops = linkedMapOf<String, Pair<Baked, BufferedImage>>()
        glyphFiles.toSortedMap().filterKeys(NotificationIcons::isBackdrop).forEach { (id, file) ->
            val image = runCatching { ImageIO.read(file) }.getOrNull() ?: return@forEach
            backdrops[NotificationIcons.glyph(id)] = bake(builder, "toast/${slug(id)}", image, nextChar(), BACKDROP_OFFSET) to image
        }
        glyphFiles.toSortedMap().filterKeys { !NotificationIcons.isBackdrop(it) }.forEach { (id, file) ->
            val image = runCatching { ImageIO.read(file) }.getOrNull()?.let(::scaled) ?: return@forEach
            icons[NotificationIcons.glyph(id)] = bake(builder, "glyph/${slug(id)}", image, nextChar(), ICON_OFFSET) to GlyphAdvance.of(image)
        }

        val pieces = listOf(left, right) + bodies.values + icons.values.map { it.first } + backdrops.values.map { it.first }
        val space = FontProvider.space(mapOf(" " to UiTextFont.SPACE_ADVANCE, ZERO_WIDTH to 0))
        tops.forEach { top ->
            val text = textFont.providers.map { FontProvider.bitMap(it.file, it.height, LineGeometry.textAscent(it.ascent, top), it.chars) }
            val glyphs = pieces.map { FontProvider.bitMap(it.texture, it.height, it.ascentOffset - top, listOf(it.char)) }
            builder.addFont(NotificationFonts.key(top), listOf<FontProvider>(space) + text + glyphs)
        }

        logger.info("[notificacoes] ${tops.size} fonte(s) de linha, ${icons.size} ícone(s), ${regions.size} região(ões).")
        return NotificationKit(
            textFont = textFont,
            textTops = tops,
            plaque = PlaqueGlyphs(left.char, right.char, bodies.mapValues { it.value.char }),
            icons = icons.mapValues { (_, baked) -> IconGlyph(baked.first.char, baked.second) },
            backdrops = backdrops.mapValues { (_, baked) -> BackdropGlyph(baked.first.char, baked.second.width, GlyphAdvance.of(baked.second)) },
        )
    }

    private fun bake(builder: PackBuilder, name: String, image: BufferedImage, char: String, ascentOffset: Int): Baked {
        val key = Key.key(NotificationFonts.NAMESPACE, "notify/$name.png")
        builder.pack.texture(Texture.texture(key, Writable.bytes(png(image))))
        return Baked(key, image.height, ascentOffset, char)
    }

    private fun scaled(image: BufferedImage): BufferedImage? =
        IconScale.native(image, LineGeometry.ICON_SIZE) ?: IconScale.resize(image, LineGeometry.ICON_SIZE)

    private fun plaqueColor(): Int {
        val image = plaqueSource?.takeIf { it.isFile }?.let { runCatching { ImageIO.read(it) }.getOrNull() }
        val sampled = image?.getRGB(image.width / 2, image.height / 2)
        return sampled?.takeIf { it ushr 24 != 0 } ?: DEFAULT_PLAQUE
    }

    private fun cap(color: Int): BufferedImage {
        val image = fill(PlaquePieces.CAP_WIDTH, color)
        image.setRGB(0, 0, 0)
        image.setRGB(0, image.height - 1, 0)
        return image
    }

    private fun fill(width: Int, color: Int): BufferedImage {
        val image = BufferedImage(width, LineGeometry.LINE_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until width) for (y in 0 until image.height) image.setRGB(x, y, color)
        return image
    }

    private fun slug(id: String): String {
        if (SLUG.matches(id)) return id
        return MessageDigest.getInstance("SHA-1").digest(id.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun png(image: BufferedImage): ByteArray =
        ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }

    companion object {
        const val FIRST_CODEPOINT = 0xF0000
        const val DEFAULT_PLAQUE = 0x42000000
        private const val PLAQUE_OFFSET = LineGeometry.BASE_ASCENT + LineGeometry.TEXT_TOP_PADDING
        private const val ICON_OFFSET = LineGeometry.BASE_ASCENT
        private const val BACKDROP_OFFSET = LineGeometry.BASE_ASCENT + LineGeometry.BACKDROP_TEXT_TOP
        private val ZERO_WIDTH = Char(0x200C).toString()
        private val SLUG = Regex("[a-z0-9_]+")
    }
}
