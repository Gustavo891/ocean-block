package ocean_block.system.shop.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.currencies.Currencies
import ocean_block.system.shop.shop.Product
import ocean_block.system.shop.shop.Shop
import org.bukkit.Material
import org.bukkit.NamespacedKey
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
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "1<<<v>>>2",
        )
    }

    override fun onOpen(open: OpenContext) {
        val prettyName = shop.get(open).prettyName
        val fillerAmount = 19 - prettyName.length
        val string = "-".repeat(fillerAmount)

        open.modifyConfig().title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:shop_category><font:oneblock:gui><shift:-170><gray>Loja ><dark_gray> $prettyName <st>$string</st>")
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = pagination.get(render)

        render.layoutSlot('1')
            .updateOnStateChange(this.pagination)
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        itemModel = NamespacedKey("oneblock", "button_left")
                        isHideTooltip = true
                    }
                }
            }
            .displayIf(BooleanSupplier { pagination.canBack() })

        render.layoutSlot('v')
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        isHideTooltip = true
                        itemModel = NamespacedKey("oneblock", "blank")
                    }
                }
            }
            .onClick { click ->
                click.player.playSound(click.player, "oneblock:button.click", 1.0f, 1.0f)
                render.openForPlayer(ShopView::class.java)
            }

        render.layoutSlot('2')
            .updateOnStateChange(this.pagination)
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        itemModel = NamespacedKey("oneblock", "button_right")
                        isHideTooltip = true
                    }
                }
            }
            .displayIf(BooleanSupplier { pagination.canAdvance() })


        render.layoutSlot('<')
            .updateOnStateChange(this.pagination)
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        isHideTooltip = true
                        itemModel = NamespacedKey("oneblock", "blank")
                    }
                }
            }
            .displayIf(BooleanSupplier { pagination.canBack() })
            .onClick { click ->
                pagination.back()
                render.update()
                click.player.playSound(click.player, "oneblock:button.click", 1.0f, 1.0f)
            }

        render.layoutSlot('>')
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        isHideTooltip = true
                        itemModel = NamespacedKey("oneblock", "blank")
                    }
                }
            }
            .updateOnStateChange(this.pagination)
            .displayIf(BooleanSupplier { pagination.canAdvance() })
            .onClick { click ->
                pagination.advance()
                render.update()
                click.player.playSound(click.player, "oneblock:button.click", 1.0f, 1.0f)
            }
    }

    private fun createProductItem(ctx: Context, product: Product): ItemStack {
        val hasCoins = Currencies.has(ctx.player.uniqueId, "coins", product.price)

        return itemStack(product.material) {
            val colorName = if (hasCoins) "<green>" else "<red>"

            meta<ItemMeta> {
                name("$colorName${product.nameSingular}")

                addLore {
                    +"<white>Preço: <green>$<white>${NumberFormatter.format(product.price)}"
                    +""
                    +"${colorName}${if (hasCoins) "Clique para adquirir!" else "Sem dinheiro!"}"
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
