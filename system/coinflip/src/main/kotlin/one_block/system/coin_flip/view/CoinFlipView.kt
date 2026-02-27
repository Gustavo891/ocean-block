package one_block.system.coin_flip.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.coin_flip.coin_flip.CoinFlip
import one_block.system.coin_flip.coin_flip.CoinFlipManager
import one_block.system.currencies.Currencies
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.meta.SkullMeta

class CoinFlipView : View() {

    private val pagination = lazyPaginationState(
        { ctx -> CoinFlipManager.toList() }
    ) { context, builder, index, value ->
        builder.renderWith { createCoinFlipItem(context, value) }
            .onClick { a ->
                handleCoinFlipClick(a, value)
            }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Coin Flip")
    }

    private fun createCoinFlipItem(ctx: Context, value: CoinFlip) = itemStack(Material.PLAYER_HEAD) {
        val sameOwner = ctx.player.uniqueId == value.creator
        val colorName = if (sameOwner) "<red>" else "<green>"

        meta<SkullMeta> {
            owningPlayer = Bukkit.getOfflinePlayer(value.creator)
            name("$colorName${value.creatorName}")

            addLore {
                +"<gray>Valor: <green>$<white>${value.amount}"
                +""
                if (sameOwner) {
                    +"<red>Sua posta!"
                } else {
                    +"<green>Clique para jogar!"
                }
            }
        }
    }

    private fun handleCoinFlipClick(ctx: Context, value: CoinFlip) {
        val player = ctx.player
        if (player.uniqueId == value.creator) return

        if (!Currencies.has(player.uniqueId, "coins", value.amount)) {
            player.sendRichMessage("<red>Sem dinheiro!.")
            return
        }

        val joinGame = CoinFlipManager.joinGame(value.uuid, player) ?: return
        Currencies.withdraw(player.uniqueId, "coins", value.amount)

        ctx.closeForPlayer()
    }
}