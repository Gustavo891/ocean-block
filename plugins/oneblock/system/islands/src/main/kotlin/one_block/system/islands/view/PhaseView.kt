package one_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.libs.formatter.Mini
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.Items.createEmptyRender
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.islands.island.IslandManager
import one_block.system.islands.phase.Phase
import one_block.system.islands.phase.PhaseManager
import one_block.textura.api.GlyphApi
import one_block.textura.api.TitleApi
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.BooleanSupplier

class PhaseView : View() {

    private val pagination = lazyPaginationState(
        { ctx -> PhaseManager.phases() }
    ) { context, builder, index, value ->
        builder.renderWith { createPhaseItem(value) }
            .onClick { click -> click.openForPlayer(PhaseDetailView::class.java, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(Mini.deserialize(BASE_TITLE))
        config.layout(
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " <  v  > "
        )
    }

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title(titleFor(open.player, FIRST_PAGE))
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = pagination.get(render)

        render.layoutSlot('<')
            .updateOnStateChange(this.pagination)
            .renderWith { arrowItem("<yellow>◀ Página anterior") }
            .displayIf(BooleanSupplier { pagination.canBack() })
            .onClick { click ->
                pagination.back()
                refreshTitle(click)
            }

        render.layoutSlot('>')
            .updateOnStateChange(this.pagination)
            .renderWith { arrowItem("<yellow>Próxima página ▶") }
            .displayIf(BooleanSupplier { pagination.canAdvance() })
            .onClick { click ->
                pagination.advance()
                refreshTitle(click)
            }

        render.createEmptyRender('v') {
            back()
        }
    }

    private fun refreshTitle(click: SlotClickContext) {
        val page = pagination.get(click).currentPageIndex()
        click.updateTitleForPlayer(titleFor(click.player, page))
    }

    private fun titleFor(player: Player, page: Int): Component {
        val builder = TitleApi.builder() ?: return Mini.deserialize(BASE_TITLE)
        val fundo = GlyphApi.getChar(BACKGROUND_GLYPH) ?: return Mini.deserialize(BASE_TITLE)

        builder.addRaw(fundo, BACKGROUND_X, DEFAULT_FONT, BACKGROUND_WIDTH)

        markedSlots(player, page).forEach { (slot, glyph) ->
            val char = GlyphApi.getChar(glyph) ?: return@forEach
            val (row, column) = slot

            builder.addRaw(char, itemX(column), rowFont(rowAscent(row)), SEAL_WIDTH)
        }

        return builder.build().white()
    }

    private fun Component.white(): Component =
        color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)

    private fun markedSlots(player: Player, page: Int): List<Pair<Pair<Int, Int>, String>> {
        val island = IslandManager.getIslandByPlayer(player.uniqueId) ?: return emptyList()
        val atual = island.getCurrentPhase() ?: return emptyList()

        val phases = PhaseManager.phases()
        val primeiro = page * PAGE_SIZE

        return (0 until PAGE_SIZE).mapNotNull { slot ->
            val phase = phases.getOrNull(primeiro + slot) ?: return@mapNotNull null

            val glyph = when {
                phase.priority < atual.priority -> COMPLETE_GLYPH
                phase.priority == atual.priority -> CURRENT_GLYPH
                else -> return@mapNotNull null
            }

            ((slot / COLUMNS) to (slot % COLUMNS + 1)) to glyph
        }
    }

    private fun createPhaseItem(value: Phase) = itemStack(value.iconMaterial) {
        try {
            meta<ItemMeta> {
                name("<yellow>${value.prettyName}")

                addLore {
                    +"<dark_gray>Blocos necessarios: ${NumberFormatter.format(value.blocksToNextPhase.toDouble())}"
                    +""
                    +"<dark_gray>Tipos de bloco: <gray>${value.blockPool.size}"
                    +"<dark_gray>Tipos de mob: <gray>${value.mobPool.size}"
                    value.chestLoot?.let { +"<dark_gray>Itens do baú: <gray>${it.entries.size}" }
                    value.monsterParty?.let { +"<dark_gray>Chefes: <gray>${it.size}" }
                    +""
                    +"<yellow>Clique para ver os detalhes."
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun arrowItem(texto: String) = itemStack(Material.ARROW) {
        meta<ItemMeta> { name(texto) }
    }

    private fun itemX(column: Int) = ITEM_OFFSET_X + column * SLOT_SIZE

    companion object {

        const val FIRST_PAGE = 0

        const val BASE_TITLE = "<white><shift:-8><glyph:island_phases>"

        const val BACKGROUND_GLYPH = "island_phases"
        const val COMPLETE_GLYPH = "hover_complete"
        const val CURRENT_GLYPH = "hover_current"

        val DEFAULT_FONT = net.kyori.adventure.key.Key.key("minecraft", "default")

        const val COLUMNS = 7
        const val ROWS = 3
        const val PAGE_SIZE = COLUMNS * ROWS

        const val SLOT_SIZE = 18

        const val ITEM_OFFSET_X = 0

        const val BACKGROUND_X = -8

        const val BACKGROUND_WIDTH = 177

        const val SEAL_WIDTH = 17

        fun rowFont(ascent: Int) = net.kyori.adventure.key.Key.key("minecraft", "font-ui-$ascent")

        fun rowAscent(row: Int) = 14 - (19 + row * SLOT_SIZE)

        fun rowAscents(): List<Int> = (0 until ROWS).map { rowAscent(it) }
    }
}
