package ocean_block.system.islands.dialog

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.currencies.Currencies
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.UpgradePurchase
import ocean_block.system.islands.view.UpgradeView
import one_block.textura.api.DialogBinds
import org.bukkit.entity.Player

object UpgradesDialog {

    private const val SCREEN = "ilha_melhorias"
    private const val COINS = "coins"

    fun open(player: Player, island: Island, viewFrame: ViewFrame) {
        IslandScreens.show(player, SCREEN, { viewFrame.open(UpgradeView::class.java, it, island) }) { binds ->
            bind(binds, player, island, viewFrame)
        }
    }

    private fun bind(binds: DialogBinds, player: Player, island: Island, viewFrame: ViewFrame) {
        binds.text("moedas") { NumberFormatter.format(Currencies.findCurrency(COINS)?.find(player.uniqueId)?.balance ?: 0.0) }
        binds.list("melhorias") {
            IslandRows.upgrades(island, { UpgradePurchase.affordable(player, it) }) { "${NumberFormatter.format(it)} MOEDAS".uppercase() }
        }
        binds.listButton("melhorar") { click, index ->
            IslandRows.upgradeAt(index)?.let { UpgradePurchase.buy(click.player, island, it) }
        }
        binds.button("voltar") { click -> IslandScreens.later(click.player) { IslandDialog.open(it, island, viewFrame) } }
    }
}
