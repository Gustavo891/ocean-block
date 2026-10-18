package one_block.textura.api

import net.kyori.adventure.key.Key
import one_block.textura.registry.GlyphRegistry

/**
 * API pública para consultar os glyphs registrados pelo plugin de resource pack.
 *
 * É inicializada em `TexturaPlugin.onEnable` via [init] e mantém a referência
 * ao [GlyphRegistry] ativo — como o registro é mutável, recargas (`/textura reload`)
 * ficam automaticamente refletidas aqui.
 */
object GlyphApi {

    @Volatile
    private var registry: GlyphRegistry? = null

    fun init(registry: GlyphRegistry) {
        this.registry = registry
    }

    /** `true` se existe um glyph com esse [id]. */
    fun has(id: String): Boolean = registry?.glyphs?.containsKey(id) == true

    /** Unicode bruto do glyph (ex.: ``), ou `null` se o id não existir. */
    fun getUnicode(id: String): String? = registry?.glyphs?.get(id)?.unicode

    /** Caractere renderizável do glyph, ou `null` se o id não existir. */
    fun getChar(id: String): String? = registry?.glyphs?.get(id)?.char

    /**
     * Caractere renderizável do glyph na fonte nomeada [font] (ex.: `"bossbar"`),
     * ou `null` se o glyph não existir ou não estiver registrado nessa fonte.
     */
    fun getChar(id: String, font: String): String? =
        registry?.glyphs?.get(id)?.fonts?.get(font)?.char

    /**
     * Chave da fonte (`minecraft:<font>`) em que o glyph [id] está registrado,
     * ou `null` se o glyph não existir ou não tiver essa fonte. O consumidor usa
     * a chave para aplicar `Component.font(...)` / `<font:...>`.
     */
    fun getFontKey(id: String, font: String): Key? =
        registry?.glyphs?.get(id)?.fonts?.get(font)?.fontKey

    /** Largura em pixels do glyph [id] (`0` = desconhecida), ou `null` se o id não existir. */
    fun getSize(id: String): Int? = registry?.glyphs?.get(id)?.size

    /**
     * Largura em pixels do glyph cujo caractere renderizável é [char], ou `null`
     * se nenhum glyph registrado usar esse char. Usado para medir texto.
     */
    fun sizeOfChar(char: String): Int? =
        registry?.sizeByChar?.get(char)

    /**
     * Caractere renderizável do glyph, ou string vazia se o id não existir —
     * conveniente para concatenar em textos sem precisar tratar `null`.
     */
    fun charOrEmpty(id: String): String = getChar(id) ?: ""

    /** Como [charOrEmpty], mas para a fonte nomeada [font]. */
    fun charOrEmpty(id: String, font: String): String = getChar(id, font) ?: ""
}
