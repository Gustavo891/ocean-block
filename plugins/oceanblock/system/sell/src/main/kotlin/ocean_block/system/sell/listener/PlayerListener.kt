package ocean_block.system.sell.listener

import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.currencies.Currencies
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.sell.Sell
import org.bukkit.Sound
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
        val sellContainer = Sell.sellContainer(event.player, clickedBlock)
        if (sellContainer == 0.0) return

        val island = IslandManager.getIslandAt(player.location) ?: return
        if(!island.isTrustAllowed(player)) {
            event.isCancelled = true
            return
        }

        Sell.updateUsesByItem(item, usesByItem - 1)
        Currencies.deposit(player.uniqueId, "coins", sellContainer)

        player.sendRichMessage("<green>Você recebeu <white>${NumberFormatter.format(sellContainer)}</white> com suas vendas.")
        player.playSound(player, Sound.ENTITY_GOAT_MILK, 1f, 1f)
    }
}
