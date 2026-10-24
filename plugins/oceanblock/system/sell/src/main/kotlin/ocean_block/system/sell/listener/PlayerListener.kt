package ocean_block.system.sell.listener

import ocean_block.system.islands.island.IslandManager
import ocean_block.system.sell.Sell
import ocean_block.system.sell.notify.SaleNotifier
import org.bukkit.Sound
import org.bukkit.block.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class PlayerListener : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        val clickedBlock = event.clickedBlock ?: return
        val item = event.item ?: return

        val usesByItem = Sell.getUsesByItem(item) ?: return
        if (clickedBlock.state !is Chest) return

        val island = IslandManager.getIslandAt(clickedBlock.location) ?: return
        if (!island.isTrustAllowed(player)) {
            event.isCancelled = true
            return
        }

        val sale = Sell.sellContainer(player, clickedBlock)
        if (sale.total <= 0.0) return

        Sell.updateUsesByItem(item, usesByItem - 1)
        SaleNotifier.notify(player, sale.total, sale.amount)
        player.playSound(player, Sound.ENTITY_GOAT_MILK, 1f, 1f)
    }
}
