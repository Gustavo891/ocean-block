package ocean_block.system.islands.listener

import ocean_block.system.islands.ext.runNextTask
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.reward.PhaseRewarder
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent

class PlayerListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerSpawnLocation(event: PlayerSpawnLocationEvent) {
        val player = event.player

        val island = IslandManager.getIslandByOwnerUUID(player.uniqueId)
        if (island != null) return

        val created = IslandManager.createIsland(player) ?: return
        val spawn = created.spawn.add(.0, 2.0, .0).toBukkit() ?: return

        event.spawnLocation = spawn
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null);
        handleIslandEntry(event.player, event.player.location)
        deliverPendingRewards(event.player)
    }

    private fun deliverPendingRewards(player: Player) {
        runNextTask(20L) {
            val owned = IslandManager.getIslandByOwnerUUID(player.uniqueId) ?: return@runNextTask
            if (owned.pendingRewardPhases.isEmpty()) return@runNextTask

            PhaseRewarder.drainPending(player, owned)
            IslandManager.markDirty(owned.uuid)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        handleIslandEntry(event.player, event.to)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        if (player.world.name != "world") return

        val islandAt = IslandManager.getIslandByPlayer(player.uniqueId) ?: return
        val respawnLoc = islandAt.spawn.toBukkit()?.add(0.5, 2.0, 0.5) ?: return

        event.respawnLocation = respawnLoc
        player.fallDistance = 0f

        runNextTask(2L) { islandAt.onJoin(player) }
    }

    private fun handleIslandEntry(player: Player, location: Location) {
        runNextTask {
            val islandAt = IslandManager.getIslandAt(location) ?: return@runNextTask
            IslandManager.playerIsland[player.uniqueId] = islandAt.uuid
            islandAt.onJoin(player)
        }
    }
}
