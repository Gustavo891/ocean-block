package one_block.textura.api

import one_block.textura.registry.TitleFontRegistry
import one_block.textura.registry.TitleFontSource

/**
 * API pública para escrever texto posicionado em pixels dentro do título de GUIs.
 *
 * É inicializada em `TexturaPlugin.onEnable` via [init] e mantém a referência ao
 * [TitleFontRegistry] ativo — como o registro é mutável, recargas (`/textura reload`)
 * ficam automaticamente refletidas aqui.
 *
 * Cada `y` usado vira uma fonte `minecraft:font-ui-<y>` gerada no resource pack.
 * Ys novos só existem no pack depois de um `/textura gerar`, então registre-os
 * cedo (via [registerAscent]) em vez de esperar o menu abrir pela primeira vez.
 */
object TitleApi {

    @Volatile
    private var registry: TitleFontRegistry? = null

    fun init(registry: TitleFontRegistry) {
        this.registry = registry
    }

    fun builder(): TitleBuilder? = registry?.let { TitleBuilder(it) }

    /**
     * Descreve um JSON de fonte próprio pra usar no título. [baseAscent] é o ascent
     * do provider principal dentro dele — informando esse valor, o `y` do título
     * significa o ascent efetivo em qualquer fonte.
     */
    fun source(path: String, baseAscent: Int): TitleFontSource = TitleFontSource(path, baseAscent)

    fun registerAscent(
        y: Int,
        height: Int? = TitleFontRegistry.BASE_HEIGHT,
        source: TitleFontSource = TitleFontSource(),
    ) {
        registry?.register(y, height, source)
    }
}
