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
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopper
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopperUpgrade
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class HopperStorageView : View() {

    private val blockState = initialState<PlacedBlock>()

    private val pagination = lazyPaginationState(
        { ctx -> blockState.get(ctx).storageItems().toMutableList() }
    ) { _, builder, _, value ->
        builder.renderWith { value }
            .onClick { click -> withdraw(click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Armazenamento")
        config.layout(
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "B <C> F I"
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('B')
            .renderWith { backItem() }
            .onClick { click ->
                val placed = blockState.get(click)
                if (!click.requireAlive(placed)) return@onClick
                click.openForPlayer(VacuumHopperView::class.java, placed)
            }

        render.layoutSlot('C')
            .renderWith { collectAllItem() }
            .onClick { click -> collectAll(click) }

        render.layoutSlot('F')
            .renderWith { filterItem(blockState.get(render)) }
            .onClick { click ->
                val placed = blockState.get(click)
                if (!click.requireAlive(placed)) return@onClick
                click.openForPlayer(HopperFilterView::class.java, placed)
            }

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

    private fun withdraw(click: SlotClickContext, value: ItemStack) {
        val placed = blockState.get(click)
        if (!click.requireAlive(placed)) return

        val stored = placed.storageItems().toMutableList()

        val index = stored.indexOfFirst { it.isSimilar(value) && it.amount == value.amount }
        if (index == -1) return

        val leftover = click.player.inventory.addItem(value.clone())
        val returned = leftover.values.firstOrNull()
        if (returned != null && returned.amount == value.amount) {
            click.player.sendRichMessage("<red>Seu inventário está cheio.")
            return
        }

        if (returned == null) {
            stored.removeAt(index)
        } else {
            stored[index] = returned
        }

        placed.setStorage(stored)
        VacuumHopper.clearFullNotice(placed)
        PlacedBlockManager.saveNow(placed)
        pagination.get(click).forceUpdate()
        click.update()
    }

    private fun collectAll(click: SlotClickContext) {
        val placed = blockState.get(click)
        if (!click.requireAlive(placed)) return

        val remaining = mutableListOf<ItemStack>()

        placed.storageItems().forEach { item ->
            remaining += click.player.inventory.addItem(item.clone()).values
        }

        placed.setStorage(remaining)
        VacuumHopper.clearFullNotice(placed)
        PlacedBlockManager.saveNow(placed)
        pagination.get(click).forceUpdate()
        click.update()

        if (remaining.isEmpty()) {
            click.player.sendRichMessage("<green>Itens coletados.")
        } else {
            click.player.sendRichMessage("<yellow>Seu inventário encheu; parte dos itens ficou no bloco.")
        }
    }

    private fun infoItem(placed: PlacedBlock, page: Pagination): ItemStack {
        val capacity = VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))
        return itemStack(Material.BOOK) {
            meta {
                name("<aqua>Armazenamento")
                setLore {
                    +"  <dark_gray>▸ <white>Itens: <aqua>${placed.storageItems().size}/$capacity"
                    +"  <dark_gray>▸ <white>Página: <aqua>${page.currentPage()}/${totalPages(page)}"
                    +""
                    +"<gray>Clique num item para retirá-lo."
                }
            }
        }
    }

    private fun collectAllItem() = itemStack(Material.HOPPER) {
        meta {
            name("<green>Coletar tudo")
            setLore {
                +"<gray>Move todos os itens guardados"
                +"<gray>para o seu inventário."
            }
        }
    }

    private fun filterItem(placed: PlacedBlock) = itemStack(Material.COMPARATOR) {
        val capacity = VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))
        meta {
            name("<gold>Filtro")
            setLore {
                +"<gray>Controla quais itens o"
                +"<gray>bloco guarda."
                +""
                +"  <dark_gray>▸ <white>Modo: <aqua>${placed.filterMode.prettyName}"
                +"  <dark_gray>▸ <white>Itens: <aqua>${placed.filter.size}"
                +"  <dark_gray>▸ <white>Capacidade: <aqua>$capacity"
                +""
                +"<yellow>Clique para abrir."
            }
        }
    }
}
