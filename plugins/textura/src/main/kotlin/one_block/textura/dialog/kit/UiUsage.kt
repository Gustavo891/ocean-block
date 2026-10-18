package one_block.textura.dialog.kit

import one_block.textura.api.DialogBinds
import one_block.textura.dialog.DialogSession
import one_block.textura.dialog.json.DefContainer
import one_block.textura.dialog.json.DefIcon
import one_block.textura.dialog.json.DefNode
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.theme.UiTheme

data class UiUsage(val images: Set<String>, val icons: Set<IconSize>, val surfaces: Set<SurfaceHeight>) {

    companion object {

        fun collect(
            screens: Collection<ScreenDefinition>,
            themes: Map<String, UiTheme>,
            kit: UiKit,
            warn: (String) -> Unit,
        ): UiUsage {
            val images = mutableSetOf<String>()
            val icons = mutableSetOf<IconSize>()
            val surfaces = mutableSetOf<SurfaceHeight>()

            screens.forEach { definition ->
                val theme = themes[definition.theme]
                if (theme == null) {
                    warn("Tela '${definition.id}': tema '${definition.theme}' não existe")
                    return@forEach
                }
                images += theme.controlArt()

                val compiler = ScreenCompiler(kit) { warn("Tela '${definition.id}': $it") }
                val pages: List<String?> = definition.pages.keys.toList().ifEmpty { listOf(null) }
                pages.forEach { page ->
                    val session = DialogSession(definition.id, DialogBinds()).also {
                        it.page = page
                        it.defaults = definition.defaults
                        it.choices = definition.choices
                    }
                    val screen = compiler.compile(definition, theme, session)
                    UiLayout(kit).layout(screen).images.forEach { placed ->
                        when (val variant = placed.variant) {
                            ImageVariant.Plain -> images += placed.id
                            is ImageVariant.Icon -> icons += IconSize(placed.id, variant.size)
                            ImageVariant.Surface -> surfaces += SurfaceHeight(placed.id, placed.height)
                        }
                    }
                }

                definition.containers().forEach { container ->
                    collectStates(container) { icon ->
                        (icon.states.values + icon.id).forEach { id -> icons += IconSize(compiler.resolveIcon(theme, id), icon.size) }
                    }
                }
            }
            return UiUsage(images, icons, surfaces)
        }

        private fun collectStates(node: DefNode, sink: (DefIcon) -> Unit) {
            when (node) {
                is DefContainer -> node.children.forEach { collectStates(it, sink) }
                is DefIcon -> if (node.states.isNotEmpty()) sink(node)
                else -> Unit
            }
        }
    }
}
