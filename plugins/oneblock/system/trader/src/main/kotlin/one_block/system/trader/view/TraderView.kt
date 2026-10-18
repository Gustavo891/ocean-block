package one_block.system.trader.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.system.currencies.Currencies
import one_block.system.trader.model.ActiveTrader
import one_block.system.trader.model.TraderItem

class TraderView : View() {

    private val traderState = initialState<ActiveTrader>()

    private val pagination = lazyPaginationState(
        { ctx -> traderState.get(ctx).shopItems }
    ) { ctx, builder, index, value ->
        builder.renderWith { createProductItem(value, ctx) }
            .onClick { ctx ->
                value.buy(ctx.player)
                ctx.parent.update()
            }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "         ",
            "  OOOOO  ",
            "  OOOOO  ",
            "         "
        )
        config.title("Mercador:")
    }


    fun createProductItem(value: TraderItem, ctx: Context) = itemStack(value.displayItem) {

        val currency = value.currency
        val player = ctx.player
        meta {
            addLore {
                +""
                +"  <white>Preço: ${getSymbol(value.id)}${NumberFormatter.format(value.price)}"
                +"  <white>Estoque: <gray>${value.stock}"
                +""
                if (value.hasStock()) {
                    if (Currencies.has(player.uniqueId, currency, value.price)) {
                        +"<green>Clique para adquiri-lo."
                    } else {
                        +"<red>Você não possui dinheiro suficiente."
                    }
                } else {
                    +"<red>Esse produto não possui estoque."
                }
            }
        }
    }

    fun getSymbol(currency: String): String {
        if (currency.equals("euro", true)) return "<gold>£"
        return "<dark_green>$<white>"

    }


}
