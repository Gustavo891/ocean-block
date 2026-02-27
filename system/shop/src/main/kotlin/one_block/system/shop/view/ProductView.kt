package one_block.system.shop.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.currencies.Currencies
import one_block.system.shop.shop.Product
import one_block.system.shop.shop.Shop
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.BooleanSupplier


class ProductView : View() {

    private val shop = initialState<Shop>()

    private val pagination = lazyPaginationState(
        { ctx -> shop.get(ctx).products.toMutableList() }
    ) { ctx, builder, index, value ->
        builder.renderWith { createProductItem(ctx, value) }
            .onClick { ctx -> ctx.openForPlayer(PurchaseView::class.java, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "         ",
            "  OOOOO  ",
            "< OOOOO >",
            "  OOOOO  ",
            "         ",
            "         "
        )
    }

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title(shop.get(open).prettyName)
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = pagination.get(render)

        render.slot(6, 5)
            .renderWith { createBackItem() }
            .onClick { ctx -> ctx.openForPlayer(ShopView::class.java) }

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
    }

    private fun createProductItem(ctx: Context, product: Product): ItemStack {
        val hasCoins = Currencies.has(ctx.player.uniqueId, "coins", product.price)

        return itemStack(product.material) {
            val colorName = if (hasCoins) "<green>" else "<red>"

            meta<ItemMeta> {
                name("$colorName${product.nameSingular}")

                addLore {
                    +"<white>Preço: <green>$<white>${product.price}"
                    +""
                    +"${colorName}${if(hasCoins) "Clique para adquirir!" else "Sem dinheiro!"}"
                }
            }
        }
    }

    private fun createBackItem() = itemStack(Material.ARROW) {
        meta<ItemMeta> {
            name("<green>Voltar!")
        }
    }
}