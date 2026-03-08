package one_block.trader.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.*
import one_block.system.currencies.Currencies
import one_block.system.currencies.currency.Currency
import one_block.system.currencies.currency.CurrencyManager
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandUpgrade
import one_block.trader.models.ActiveTrader
import one_block.trader.models.TraderItem
import org.bukkit.inventory.ItemStack

class TraderView: View() {

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
//        config.title(
//            MiniMessage.miniMessage()
//                .deserialize("<shift:-8><glyph:island_upgrade><font:oneblock:gui><shift:-170><gray>Ilha > <dark_gray>Melhorias")
//        )
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
                if(value.hasStock()) {
                    if(Currencies.has(player.uniqueId, currency, value.price)) {
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

    fun getSymbol(currency: String) : String {
        if(currency.equals("euro", true)) return "<gold>£"
        return "<dark_green>$<white>"

    }



}