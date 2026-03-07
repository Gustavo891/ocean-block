package one_block.system.auction_house.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.auction_house.auction.Auction
import one_block.system.auction_house.auction.AuctionManager
import one_block.system.currencies.Currencies
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.BooleanSupplier

class AuctionView : View() {

    private val pagination = lazyPaginationState(
        { ctx -> AuctionManager.auctions.values.toMutableList() }
    ) { ctx, builder, index, value ->
        builder.renderWith { createAuctionItem(ctx, value) }
            .onClick { ctx -> handleAuctionClick(ctx, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Mercado")
        config.layout(
            "         ",
            "  OOOOO  ",
            "< OOOOO >",
            "  OOOOO  ",
            "         ",
            "         "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = pagination.get(render)

        render.layoutSlot('<')
            .updateOnStateChange(this.pagination)
            .renderWith { ItemStack(Material.ARROW) }
            .displayIf(BooleanSupplier { pagination.canBack() })
            .onClick(pagination::back)

        render.layoutSlot('>')
            .renderWith { ItemStack(Material.ARROW) }
            .updateOnStateChange(this.pagination)
            .displayIf(BooleanSupplier { pagination.canAdvance() })
            .onClick(pagination::advance)

        render.slot(3, 5)
            .renderWith { createEmptyItem() }
            .displayIf(BooleanSupplier { AuctionManager.auctions.values.isEmpty() })
    }

    private fun createAuctionItem(ctx: Context, auction: Auction) = itemStack(auction.item) {
        meta<ItemMeta> {
            addLore {
                +"<white>Vendedor: ${auction.sellerName}"
                +"<white>Preço: <green>$<white>${auction.price}"
                +"<white>Expira em:"
                +""
                +"<green>Clique para comprar!"
            }
        }
    }

    private fun handleAuctionClick(ctx: SlotClickContext, auction: Auction) {
        val player = ctx.player
        if (!Currencies.has(player.uniqueId, "coins", auction.price)) {
            player.sendRichMessage("<red>Sem dinheiro!")
            return
        }

// verificar se o inventario está cheio

        val removed = AuctionManager.unregister(auction.uuid) ?: return
        Currencies.withdraw(player.uniqueId, "coins", removed.price)
        Currencies.deposit(removed.sellerUuid, "coins", removed.price)

        pagination.get(ctx).forceUpdate()
        player.playSound(player, Sound.ENTITY_ARMADILLO_BRUSH, 1.0f, 1.0f)
        player.sendRichMessage("<green>Você adquiriu por <green>$<white>${removed.price}<green>.")

        player.inventory.addItem(removed.item)
    }

    private fun createEmptyItem() = itemStack(Material.COBWEB) {
        meta<ItemMeta> {
            name("<white>Vazio!")

            addLore {
                +"<white>Nenhum item aqui!"
            }
        }
    }

    private fun createBackItem() = itemStack(Material.ARROW) {
        meta<ItemMeta> {
            name("<green>Voltar!")
        }
    }

}