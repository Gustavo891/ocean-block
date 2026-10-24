package textura_studio

import one_block.textura.dialog.kit.UiArtCatalog
import textura_studio.render.GlyphPreview
import textura_studio.render.GlyphRequest
import textura_studio.render.HudPreview
import textura_studio.render.HudRequest
import textura_studio.render.HudResult
import textura_studio.render.PreviewRequest
import textura_studio.render.PreviewResult
import textura_studio.render.ScreenPreview
import textura_studio.render.StudioAssets
import java.awt.image.BufferedImage
import java.io.File

data class ThemeInfo(val styles: List<String>, val surfaces: List<String>, val icons: List<String>, val looks: List<String>, val iconSize: Int)

/** O que o painel oferece nos seletores: estilos e superfícies do tema, artes, glyphs e itens vanilla. */
data class AssetCatalog(val themes: Map<String, ThemeInfo>, val ui: List<String>, val glyphs: List<String>, val items: List<String>)

/** Fachada do painel: arquivos de tela + motor de preview sobre a pasta de dados do Textura. */
class Studio(val data: File) {

    val store = DialogStore(File(data, "configs/ui"))

    val files = StudioFiles(data)

    val blocks = BlockCatalog(data)

    @Volatile
    private var cached: StudioAssets? = null

    @Volatile
    private var items: List<String>? = null

    fun assets(): StudioAssets = cached ?: synchronized(this) {
        cached ?: StudioAssets.load(data).also { cached = it }
    }

    /** Relê temas, artes, fontes e glyphs (depois de mexer nos arquivos do pack). */
    fun reload() {
        synchronized(this) {
            cached = null
            items = null
        }
    }

    fun themes(): List<String> = assets().themes.keys.sorted()

    fun preview(request: PreviewRequest): PreviewResult = ScreenPreview(assets()).render(request)

    fun hudPreview(request: HudRequest): HudResult = HudPreview(assets(), File(data, HUD_PLAQUE)).render(request)

    fun glyphPreview(request: GlyphRequest): BufferedImage = GlyphPreview(assets()).render(request)

    fun catalog(): AssetCatalog {
        val assets = assets()
        val themes = assets.themes.mapValues { (_, theme) ->
            ThemeInfo(
                styles = theme.styles.keys.sorted(),
                surfaces = theme.surfaces.keys.sorted(),
                icons = theme.icons.keys.sorted(),
                looks = theme.button.looks.keys.sorted(),
                iconSize = theme.iconSize,
            )
        }
        val vanilla = items ?: assets.files.jarNames(ITEM_TEXTURES, ".png").also { items = it }
        return AssetCatalog(themes, assets.uiArt, assets.glyphIds.map { UiArtCatalog.GLYPH_PREFIX + it }, vanilla)
    }

    /** Imagem de uma arte (`ui:`, `glyph:`, `item:` ou nome de ícone do tema). */
    fun art(id: String, theme: String?): BufferedImage? {
        val assets = assets()
        assets.catalog.art(id)?.let { return it.image }
        val icons = theme?.let { assets.themes[it] }?.icons ?: return null
        val alias = icons[id.removePrefix(UiArtCatalog.UI_PREFIX)] ?: return null
        val resolved = if (alias.contains(':')) alias else UiArtCatalog.GLYPH_PREFIX + alias
        return assets.catalog.art(resolved)?.image
    }

    private companion object {
        const val ITEM_TEXTURES = "assets/minecraft/textures/item/"
        const val HUD_PLAQUE = "resource/assets/oneblock/textures/hud/body.png"
    }
}
