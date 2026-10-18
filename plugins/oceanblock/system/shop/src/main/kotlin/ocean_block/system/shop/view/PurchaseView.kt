package ocean_block.system.shop.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.currencies.Currencies
import ocean_block.system.shop.shop.Product
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class PurchaseView : View() {

    private val product = initialState<Product>()
    private val currentAmount = mutableState(1)

    private val pagination = computedPaginationState(
        { mutableListOf(1, 5, 10, 30, 64) }
    ) { context, builder, index, value ->
        builder.renderWith { createAmountItem(context, value, product.get(context).material) }
            .onClick { click ->
                handleAmountClick(context, value)
                click.update()
            }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "  OOOOO  ",
            "         "
        )
    }

    override fun onOpen(open: OpenContext) {
        val name = product.get(open).nameSingular
        val avail = 16
        val display = if (name.length > avail) name.take(avail - 3) + "..." else name

        open.modifyConfig().title(
            Mini.mm.deserialize("<white><shift:-8><glyph:shop_product><font:oneblock:gui><shift:-170><gray>Comprando > <dark_gray>$display")
        )
    }

    override fun onFirstRender(render: RenderContext) {

        val product = product.get(render)

        render.slot(2, 4)
            .renderWith { createBackItem() }
            .onClick { ctx -> ctx.back() }

        render.slot(2, 6)
            .watch(currentAmount)
            .renderWith { createConfirmationItem(render) }
            .onClick { ctx -> handleConfirmationClick(ctx, currentAmount.get(render)) }

        render.slot(2, 5)
            .renderWith { itemStack(product.material) {
                meta<ItemMeta> {
                    name("<white>${product.nameSingular}")
                    amount = currentAmount.get(render)
                }
            } }
    }

    private fun createAmountItem(ctx: Context, quantidade: Int, tipo: Material): ItemStack {
        val get = currentAmount.get(ctx)

        return itemStack(if (get == quantidade) Material.LIME_STAINED_GLASS_PANE else Material.GRAY_STAINED_GLASS_PANE) {
            val product = product.get(ctx)

            meta<ItemMeta> {
                amount = quantidade
                name("<green>${quantidade} ${if (quantidade == 1) product.nameSingular else product.namePlural}")
            }
        }
    }

    private fun handleAmountClick(ctx: Context, amount: Int) {
        currentAmount.set(amount, ctx)
        pagination.get(ctx).forceUpdate()
    }

    private fun createConfirmationItem(ctx: Context) = itemStack(Material.PAPER) {
        val product = product.get(ctx)
        val amount = currentAmount.get(ctx)
        val total = product.price * amount

        if (!Currencies.has(ctx.player.uniqueId, "coins", total)) {
            return createInsufficientCoinsItem(ctx, product, amount, total)
        }

        meta<ItemMeta> {
            name("<green>Confirmar!")
            itemModel = NamespacedKey("oneblock", "accept")
            addLore {
                +"<gray>Você está comprando"
                +"<white>${amount} ${if (amount == 1) product.nameSingular else product.namePlural} <gray>por "
                +"<green>$<white>${total} ${if (total == 1.0) "<gray>coin" else "<gray>coins"}"
                +""
                +"<green>Clique para adquirir!"
            }
        }
    }

    private fun createInsufficientCoinsItem(ctx: Context, product: Product, amount: Int, total: Double) =
        itemStack(Material.PAPER) {
            meta<ItemMeta> {
                name("<red>Sem dinheiro!")
                itemModel = NamespacedKey("oneblock", "deny")
                addLore {
                    +"<gray>Você está comprando"
                    +"<white>${amount} ${if (amount == 1) product.nameSingular else product.namePlural} <gray>por "
                    +"<green>$<white>${total} ${if (total == 1.0) "<gray>coin" else "<gray>coins"}"
                }
            }
        }

    private fun handleConfirmationClick(click: SlotClickContext, amount: Int) {
        val player = click.player
        val product = product.get(click)
        val totalPrice = product.price * amount

        if (!Currencies.has(player.uniqueId, "coins", totalPrice)) {
            return
        }

        Currencies.withdraw(player.uniqueId, "coins", totalPrice)

        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
        player.inventory.addItem(ItemStack(product.material, amount))

        player.sendRichMessage("<green>Produto adquirido com sucesso.")
    }

    private fun createBackItem() = itemStack(Material.PAPER) {
        meta<ItemMeta> {
            name("<green>Voltar!")
            itemModel = NamespacedKey("oneblock", "blank")
        }
    }
}
