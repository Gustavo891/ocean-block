package ocean_block.system.drift.hook

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class FirstJoinListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPlayedBefore()) return

        player.inventory.addItem(GanchoItem.build())
    }
}
