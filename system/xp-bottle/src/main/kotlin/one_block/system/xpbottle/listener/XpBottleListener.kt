package one_block.system.xpbottle.listener

import one_block.system.xpbottle.XpBottleItem
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class XpBottleListener : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!event.action.name.contains("RIGHT_CLICK")) return

        val player = event.player
        val item = event.item ?: return

        val levels = XpBottleItem.getLevels(item)
        if (levels == 0) return

        event.isCancelled = true

        player.giveExp(levels)
        item.amount -= 1

        player.sendRichMessage("<green>Você recuperou $levels níveis.")
    }
}