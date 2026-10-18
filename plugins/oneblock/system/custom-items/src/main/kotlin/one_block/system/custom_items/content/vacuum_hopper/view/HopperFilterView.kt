package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.block.FilterMode
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

class HopperFilterView : View() {

    private val blockState = initialState<PlacedBlock>()

    private val pagination = lazyPaginationState(
        { ctx ->
            blockState.get(ctx).filter
                .mapNotNull { runCatching { Material.valueOf(it) }.getOrNull() }
                .toMutableList()
        }
    ) { _, builder, _, material ->
        builder.renderWith { filterEntryItem(material) }
            .onClick { click -> removeFromFilter(click, material) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Filtro do Vacuum Hopper")
        config.layout(
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "         ",
            "B  <M>  I"
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('B')
            .renderWith { backItem() }
            .onClick { click ->
                val placed = blockState.get(click)
                if (!click.requireAlive(placed)) return@onClick
                click.openForPlayer(HopperStorageView::class.java, placed)
            }

        render.layoutSlot('M')
            .renderWith { modeItem(blockState.get(render)) }
            .onClick { click -> cycleMode(click) }

        render.layoutSlot('I')
            .renderWith { infoItem(blockState.get(render), pagination.get(render)) }

        render.layoutSlot('<')
            .displayIf { ctx -> pagination.get(ctx).canBack() }
            .renderWith { pageItem("<yellow>← Página anterior", pagination.get(render)) }
            .onClick { click -> switchPage(click) { page -> page.back() } }

        render.layoutSlot('>')
            .displayIf { ctx -> pagination.get(ctx).canAdvance() }
            .renderWith { pageItem("<yellow>Próxima página →", pagination.get(render)) }
            .onClick { click -> switchPage(click) { page -> page.advance() } }
    }

    private fun switchPage(click: SlotClickContext, action: (Pagination) -> Unit) {
        action(pagination.get(click))
        click.update()
    }

    private fun pageItem(title: String, page: Pagination) = itemStack(Material.SPECTRAL_ARROW) {
        meta {
            name(title)
            setLore { +"<gray>Página <white>${page.currentPage()}<gray> de <white>${totalPages(page)}" }
        }
    }

    private fun totalPages(page: Pagination): Int = page.lastPage().coerceAtLeast(1)

    private fun backItem() = itemStack(Material.ARROW) {
        meta { name("<gray>← Voltar") }
    }

    override fun onClick(click: SlotClickContext) {
        if (click.clickOrigin.clickedInventory?.type != InventoryType.PLAYER) return

        val item = click.clickOrigin.currentItem ?: return
        if (item.type.isAir) return

        val placed = blockState.get(click)
        if (!click.requireAlive(placed)) return
        if (!placed.filter.add(item.type.name)) return

        PlacedBlockManager.saveNow(placed)
        refresh(click)
    }

    private fun removeFromFilter(click: SlotClickContext, material: Material) {
        val placed = blockState.get(click)
        if (!click.requireAlive(placed)) return

        placed.filter.remove(material.name)
        PlacedBlockManager.saveNow(placed)
        refresh(click)
    }

    private fun cycleMode(click: SlotClickContext) {
        val placed = blockState.get(click)
        if (!click.requireAlive(placed)) return

        placed.filterMode = placed.filterMode.next()
        PlacedBlockManager.saveNow(placed)
        refresh(click)
    }

    private fun refresh(click: SlotClickContext) {
        pagination.get(click).forceUpdate()
        click.update()
    }

    private fun filterEntryItem(material: Material) = itemStack(material) {
        meta {
            name("<white>${material.name.lowercase().replace('_', ' ')}")
            setLore { +"<red>Clique para remover da lista." }
        }
    }

    private fun modeItem(placed: PlacedBlock): ItemStack {
        val mode = placed.filterMode
        val material = if (mode == FilterMode.WHITELIST) Material.LIME_DYE else Material.RED_DYE
        return itemStack(material) {
            meta {
                name("<yellow>Modo: <white>${mode.prettyName}")
                setLore {
                    +"<gray>${mode.description}"
                    +""
                    FilterMode.entries.forEach { option ->
                        if (option == mode) {
                            +"<green>▸ <white>${option.prettyName}"
                        } else {
                            +"<dark_gray>  ${option.prettyName}"
                        }
                    }
                    +""
                    +"<yellow>Clique para alternar."
                }
            }
        }
    }

    private fun infoItem(placed: PlacedBlock, page: Pagination) = itemStack(Material.BOOK) {
        meta {
            name("<aqua>Como usar")
            setLore {
                +"<gray>Clique num item do seu"
                +"<gray>inventário para adicioná-lo"
                +"<gray>à lista de filtro."
                +""
                +"<gray>Clique num item do topo"
                +"<gray>para removê-lo."
                +""
                +"  <dark_gray>▸ <white>Itens: <aqua>${placed.filter.size}"
                +"  <dark_gray>▸ <white>Página: <aqua>${page.currentPage()}/${totalPages(page)}"
            }
        }
    }
}
