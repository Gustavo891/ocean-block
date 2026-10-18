package ocean_block.system.shop.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.shop.shop.Shop
import ocean_block.system.shop.shop.ShopManager
import org.bukkit.inventory.meta.ItemMeta

class ShopView : View() {

    private val pagination = computedPaginationState(
        { ShopManager.toList() }
    ) { context, builder, index, value ->
        builder.withItem(createShopItem(value))
            .onClick { a -> a.openForPlayer(ProductView::class.java, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(Mini.deserialize("<white><shift:-8><glyph:shop>"))
        config.layout(
            "  OOOOO  ",
            "  OOOOO  ",

        )
    }

    private fun createShopItem(shop: Shop) = itemStack(shop.icon) {
        meta<ItemMeta> {
            name("<green>${shop.prettyName}")

            addLore {
                shop.description.forEach { +it }
            }
        }
    }
}
