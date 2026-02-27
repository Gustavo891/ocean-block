package one_block.system.tops.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import one_block.libs.formatter.NumberFormatter
import one_block.libs.formatter.TimeFormatter
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.currencies.Currencies
import one_block.system.playtime.player_playtime.PlayerPlaytimeManager
import one_block.system.tops.PlayerTop
import one_block.system.tops.Top
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.concurrent.TimeUnit

class TopsView : View() {

    private val pagination = computedPaginationState(
        { ctx -> Top.entries.toMutableList() }
    ) { context, builder, index, value ->
        builder.renderWith { createPlayerTopItem(context, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Tops")
        config.layout(
            "         ",
            "  OOOOO  ",
            "  OOOOO  ",
            "  OOOOO  ",
            "         "
        )
    }

    private fun createPlayerTopItem(ctx: Context, value: Top): ItemStack {

        return itemStack(value.iconMaterial) {
            meta<ItemMeta> {
                name("${value.prettyName}")

                addLore {
                    val tops = tops(value)
                    if (tops.isEmpty()) {
                        +"<white>Ninguem..."
                    }

                    for (player in tops) {
                        +"<white>${player.name}: <gray>${player.value}"
                    }
                }
            }
        }
    }

    private fun tops(top: Top): List<PlayerTop> {
        return when (top) {
            Top.Coins, Top.Tokens -> (Currencies.findCurrency(top.name.lowercase())?.topWallets() ?: emptyList())
                .map { PlayerTop(it.name, NumberFormatter.format(it.balance)) }

            Top.Playtime -> PlayerPlaytimeManager.topPlaytimeList()
                .map { PlayerTop(it.name, TimeFormatter.formatReduced(TimeUnit.SECONDS.toMillis(it.playtime))) }
        }
    }

}