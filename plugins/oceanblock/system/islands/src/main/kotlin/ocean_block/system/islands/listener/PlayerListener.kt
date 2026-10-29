package ocean_block.system.islands.listener

import ocean_block.system.islands.ext.runNextTask
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.type.IslandType
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

        val home = IslandManager.getHomeIsland(player)
        if (home != null) {
            if (event.spawnLocation.world?.name != ISLAND_WORLD) {
                home.spawn.add(.0, 2.0, .0).toBukkit()?.let { event.spawnLocation = it }
            }
            return
        }

        val created = IslandManager.createIsland(player, IslandType.PADRAO) { ready -> runNextTask { enterNewIsland(player, ready) } } ?: return
        val spawn = created.spawn.add(.0, 2.0, .0).toBukkit() ?: return

        event.spawnLocation = spawn
    }

    private fun enterNewIsland(player: Player, island: Island) {
        if (!player.isOnline) return
        val spawn = island.spawn.toBukkit() ?: return

        player.teleport(spawn.add(0.0, 2.0, 0.0))
        player.fallDistance = 0f
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
        if (event.isBedSpawn || event.isAnchorSpawn) return

        val islandAt = IslandManager.getHomeIsland(player) ?: return
        val respawnLoc = islandAt.spawn.toBukkit()?.add(0.0, 2.0, 0.0) ?: return

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

private const val ISLAND_WORLD = "world"
