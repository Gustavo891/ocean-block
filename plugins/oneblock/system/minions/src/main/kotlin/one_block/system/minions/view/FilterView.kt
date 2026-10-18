package one_block.system.minions.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.minions.enums.FilterMode
import one_block.system.minions.placed_minion.MinionFilter
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

class FilterView : View() {

    private val minionState = initialState<PlacedMinion>()

    private val pagination = lazyPaginationState(
        { ctx -> minionState.get(ctx).filter.filter { MinionFilter.itemOf(it) != null }.toMutableList() }
    ) { _, builder, _, entry ->
        builder.renderWith { filterEntryItem(entry) }
            .onClick { click -> removeFromFilter(click, entry) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Filtro do Minion")
        config.layout(
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "         ",
            "B   M   I"
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('B')
            .renderWith { backItem() }
            .onClick { click -> click.openForPlayer(MinionView::class.java, minionState.get(click)) }

        render.layoutSlot('M')
            .renderWith { modeItem(minionState.get(render)) }
            .onClick { click -> cycleMode(click) }

        render.layoutSlot('I')
            .renderWith { infoItem() }
    }

    override fun onClick(click: SlotClickContext) {
        if (click.clickOrigin.clickedInventory?.type != InventoryType.PLAYER) return

        val item = click.clickOrigin.currentItem ?: return
        if (item.type.isAir) return

        val minion = minionState.get(click)
        if (minion.filter.any { MinionFilter.matches(it, item) }) return

        minion.filter.add(MinionFilter.entryOf(item))
        PlacedMinionManager.update(minion)
        pagination.get(click).forceUpdate()
    }

    private fun removeFromFilter(click: SlotClickContext, entry: String) {
        val minion = minionState.get(click)
        minion.filter.remove(entry)
        PlacedMinionManager.update(minion)
        pagination.get(click).forceUpdate()
    }

    private fun cycleMode(click: SlotClickContext) {
        val minion = minionState.get(click)
        minion.filterMode = minion.filterMode.next()
        PlacedMinionManager.update(minion)
        pagination.get(click).forceUpdate()
    }

    private fun filterEntryItem(entry: String): ItemStack {
        val base = MinionFilter.itemOf(entry) ?: ItemStack(Material.BARRIER)

        return itemStack(base) {
            meta {
                if (!hasDisplayName()) {
                    name("<white>${base.type.name.lowercase().replace('_', ' ')}")
                }
                addLore {
                    +""
                    +"<red>Clique para remover da lista."
                }
            }
        }
    }

    private fun modeItem(minion: PlacedMinion): ItemStack {
        val mode = minion.filterMode
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

    private fun infoItem() = itemStack(Material.BOOK) {
        meta {
            name("<aqua>Como usar")
            setLore {
                +"<gray>Clique num item do seu"
                +"<gray>inventário para adicioná-lo"
                +"<gray>à lista de filtro."
                +""
                +"<gray>Clique num item do topo"
                +"<gray>para removê-lo."
            }
        }
    }

    private fun backItem() = itemStack(Material.ARROW) {
        meta { name("<gray>← Voltar") }
    }
}
