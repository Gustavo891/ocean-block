package one_block.textura.dialog.kit

import one_block.textura.api.DialogBinds
import one_block.textura.dialog.DialogSession
import one_block.textura.dialog.json.DefButton
import one_block.textura.dialog.json.DefChip
import one_block.textura.dialog.json.DefContainer
import one_block.textura.dialog.json.DefIcon
import one_block.textura.dialog.json.DefNav
import one_block.textura.dialog.json.DefNode
import one_block.textura.dialog.json.DefRepeat
import one_block.textura.dialog.json.DefScoped
import one_block.textura.dialog.json.DefSlots
import one_block.textura.dialog.json.DefText
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.json.ScreenParser
import one_block.textura.dialog.json.SlotContent
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.theme.UiTheme

data class UiUsage(val images: Set<String>, val icons: Set<IconSize>, val surfaces: Set<SurfaceHeight>) {

    companion object {

        fun collect(
            screens: Collection<ScreenDefinition>,
            themes: Map<String, UiTheme>,
            kit: UiKit,
            dynamicItems: Set<String> = DynamicItemIcons.all(),
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

                val screenWarn: (String) -> Unit = { warn("Tela '${definition.id}': $it") }
                val compiler = ScreenCompiler(kit, screenWarn)
                val samples = ListSamples.combinations(ListSamples.limits(definition, screenWarn), screenWarn)
                val pages: List<String?> = definition.pages.keys.toList().ifEmpty { listOf(null) }
                pages.forEach { page ->
                    samples.forEach { counts ->
                        val session = DialogSession(definition.id, sampleBinds(counts)).also {
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
                }

                definition.containers().forEach { container ->
                    collectStates(container) { icon ->
                        (icon.states.values + icon.id).forEach { id -> icons += IconSize(compiler.resolveIcon(theme, id), icon.size) }
                    }
                }
                if (definition.containers().any(::hasDynamicSlot)) {
                    val size = compiler.slotIconSize(theme)
                    dynamicItems.forEach { item -> icons += IconSize(compiler.resolveIcon(theme, UiArtCatalog.ITEM_PREFIX + item), size) }
                }
                if (definition.containers().any(::hasDynamicIcon)) icons += dynamicLabelIcons(theme, compiler, dynamicItems)
            }
            return UiUsage(images, icons, surfaces)
        }

        private fun sampleBinds(counts: Map<String, Int>): DialogBinds = DialogBinds().apply {
            counts.forEach { (key, count) -> list(key) { List(count) { emptyMap() } } }
        }

        private fun dynamicLabelIcons(theme: UiTheme, compiler: ScreenCompiler, dynamicItems: Set<String>): Set<IconSize> =
            (theme.icons.keys.map { compiler.resolveIcon(theme, UiArtCatalog.UI_PREFIX + it) } + dynamicItems.map { UiArtCatalog.ITEM_PREFIX + it })
                .map { IconSize(it, theme.iconSize) }
                .toSet()

        private fun hasDynamicIcon(node: DefNode): Boolean = when (node) {
            is DefContainer -> node.children.any(::hasDynamicIcon)
            is DefRepeat -> hasDynamicIcon(node.node)
            is DefScoped -> hasDynamicIcon(node.node)
            is DefNav -> dynamic(node.icon)
            is DefChip -> dynamic(node.icon)
            is DefButton -> dynamic(node.icon)
            is DefText -> dynamic(node.icon)
            else -> false
        }

        private fun dynamic(icon: String?): Boolean = icon != null && ScreenParser.PLACEHOLDER.containsMatchIn(icon)

        private fun hasDynamicSlot(node: DefNode): Boolean = when (node) {
            is DefContainer -> node.children.any(::hasDynamicSlot)
            is DefRepeat -> hasDynamicSlot(node.node)
            is DefScoped -> hasDynamicSlot(node.node)
            is DefSlots -> node.items.any(SlotContent::dynamic)
            else -> false
        }

        private fun collectStates(node: DefNode, sink: (DefIcon) -> Unit) {
            when (node) {
                is DefContainer -> node.children.forEach { collectStates(it, sink) }
                is DefRepeat -> collectStates(node.node, sink)
                is DefScoped -> collectStates(node.node, sink)
                is DefIcon -> if (node.states.isNotEmpty()) sink(node)
                else -> Unit
            }
        }
    }
}
