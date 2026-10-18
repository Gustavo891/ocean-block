package one_block.textura.pack

import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.overlay.ResourceContainer
import team.unnamed.creative.overlay.ResourceContainerImpl
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter
import team.unnamed.creative.texture.Texture
import java.io.File


class PackBuilder(
    private val packFormat: Int = DEFAULT_PACK_FORMAT,
    description: String = DEFAULT_DESCRIPTION
) {
    val pack: ResourcePack = ResourcePack.resourcePack().apply {
        packMeta(packFormat, description)
    }

    /**
     * @param writeTexture `false` quando o PNG já foi copiado de `resource/` — o
     * creative recusa dois arquivos no mesmo caminho do zip.
     */
    fun addGlyph(
        textureKey: Key,
        texture: Writable,
        char: String,
        height: Int = DEFAULT_GLYPH_HEIGHT,
        ascent: Int = DEFAULT_GLYPH_ASCENT,
        fontKey: Key = Font.MINECRAFT_DEFAULT,
        writeTexture: Boolean = true
    ) = addGlyphSheet(textureKey, texture, listOf(char), height, ascent, fontKey, writeTexture)

    /**
     * Folha de glyphs: um único provider com N entradas em [chars], que é como o
     * cliente fatia a textura em N linhas de mesma altura.
     */
    fun addGlyphSheet(
        textureKey: Key,
        texture: Writable,
        chars: List<String>,
        height: Int = DEFAULT_GLYPH_HEIGHT,
        ascent: Int = DEFAULT_GLYPH_ASCENT,
        fontKey: Key = Font.MINECRAFT_DEFAULT,
        writeTexture: Boolean = true
    ) {
        if (writeTexture && pack.texture(textureKey) == null) {
            pack.texture(Texture.texture(textureKey, texture))
        }
        val provider = FontProvider.bitMap(textureKey, height, ascent, chars)
        val existing = pack.font(fontKey)?.providers().orEmpty()
        pack.font(Font.font(fontKey, existing + provider))
    }

    fun addFont(fontKey: Key, providers: List<FontProvider>) {
        pack.font(Font.font(fontKey, providers))
    }

    fun build(output: File): File {
        output.parentFile?.mkdirs()
        MinecraftResourcePackWriter.minecraft().writeToZipFile(output, pack)
        PackMcmeta.declareFormatRange(output, packFormat)
        return output
    }

    companion object {
        // Tem que bater com a versao do cliente: abaixo disso o MC trata o pack como
        // de outra versao e as fontes customizadas param de valer. 97 = 26.3.
        const val DEFAULT_PACK_FORMAT = 97
        const val DEFAULT_DESCRIPTION = "Resource Pack Gerado Automaticamente"
        const val DEFAULT_GLYPH_HEIGHT = 8
        const val DEFAULT_GLYPH_ASCENT = 8
    }
}