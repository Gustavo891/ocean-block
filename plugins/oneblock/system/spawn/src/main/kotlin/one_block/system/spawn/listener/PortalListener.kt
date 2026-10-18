package one_block.system.spawn.listener

import one_block.system.spawn.portal.IslandPortal
import one_block.system.spawn.spawn.SpawnManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause

class PortalListener : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPortal(event: PlayerPortalEvent) {
        if (event.cause != TeleportCause.END_PORTAL) return
        if (!SpawnManager.isAtSpawn(event.player)) return

        event.isCancelled = true
        IslandPortal.sendHome(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = IslandPortal.forget(event.player.uniqueId)
}
