package ocean_block.system.spawn.listener

import ocean_block.system.islands.hud.IslandHud
import ocean_block.system.spawn.spawn.SpawnManager
import one_block.textura.api.HudApi
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent

class SpawnHudListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        if (!SpawnManager.isAtSpawn(event.player)) return
        HudApi.hide(event.player, IslandHud.SLOT)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onChangeWorld(event: PlayerChangedWorldEvent) {
        if (!SpawnManager.isAtSpawn(event.player)) return
        HudApi.hide(event.player, IslandHud.SLOT)
    }
}
