package ocean_block.system.drift.net.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.drift.net.NetBlock
import ocean_block.system.drift.net.NetFullNotifier
import ocean_block.system.drift.net.NetIndex
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class NetStorageView : View() {

    private val blockState = initialState<PlacedBlock>()

    private val pagination = lazyPaginationState(
        { ctx -> blockState.get(ctx).storageItems().toMutableList() }
    ) { _, builder, _, value ->
        builder.renderWith { value }
            .onClick { click -> withdraw(click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Rede de Detritos")
        config.layout(
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "   C I   ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('C')
            .renderWith { collectAllItem() }
            .onClick { click -> collectAll(click) }

        render.layoutSlot('I')
            .renderWith { infoItem(blockState.get(render)) }
    }

    private fun withdraw(click: SlotClickContext, value: ItemStack) {
        val placed = blockState.get(click)
        if (!click.requireAccess(placed)) return

        val stored = placed.storageItems()
        val index = stored.indexOfFirst { it.isSimilar(value) && it.amount == value.amount }
        if (index == -1) return refresh(click)

        val returned = click.player.inventory.addItem(value.clone()).values.firstOrNull()
        if (returned != null && returned.amount == value.amount) {
            click.player.sendRichMessage("<red>Seu inventário está cheio.")
            return
        }

        val updated = if (returned == null) {
            stored.filterIndexed { position, _ -> position != index }
        } else {
            stored.mapIndexed { position, item -> if (position == index) returned else item }
        }
        save(placed, updated)
        refresh(click)
    }

    private fun collectAll(click: SlotClickContext) {
        val placed = blockState.get(click)
        if (!click.requireAccess(placed)) return

        val remaining = placed.storageItems().flatMap { click.player.inventory.addItem(it.clone()).values }
        save(placed, remaining)
        refresh(click)

        if (remaining.isEmpty()) {
            click.player.sendRichMessage("<green>Itens coletados.")
        } else {
            click.player.sendRichMessage("<yellow>Seu inventário encheu; parte dos itens ficou na rede.")
        }
    }

    private fun save(placed: PlacedBlock, items: List<ItemStack>) {
        placed.setStorage(items)
        PlacedBlockManager.saveNow(placed)
        NetIndex.refresh(placed)
        NetFullNotifier.afterChange(placed)
    }

    private fun refresh(click: SlotClickContext) {
        pagination.get(click).forceUpdate()
        click.update()
    }

    private fun infoItem(placed: PlacedBlock): ItemStack = itemStack(Material.BOOK) {
        meta {
            name("<aqua>Rede de Detritos")
            setLore {
                +"  <dark_gray>▸ <white>Pilhas: <aqua>${placed.storageItems().size}/${NetBlock.CAPACITY}"
                +""
                +"<gray>Clique num item para retirá-lo."
            }
        }
    }

    private fun collectAllItem(): ItemStack = itemStack(Material.HOPPER) {
        meta {
            name("<green>Coletar tudo")
            setLore {
                +"<gray>Move todos os itens guardados"
                +"<gray>para o seu inventário."
            }
        }
    }
}
