package one_block.system.islands.listener

import one_block.system.islands.bossbar.BossbarManager
import one_block.system.islands.ext.runNextTask
import one_block.system.islands.island.IslandManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent

class PlayerListener : Listener {

    @EventHandler
    fun onPlayerSpawnLocation(event: PlayerSpawnLocationEvent) {
        val player = event.player

        var island = IslandManager.islandsById.values.find { it.ownerUuid == player.uniqueId }
        if (island == null) {
            island = IslandManager.createIsland(player)

            val spawn = island.spawn.add(.0, 2.0, .0).toBukkit()!!
            event.spawnLocation = spawn
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null);
        handleIslandEntry(event.player, event.player.location)
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val island = IslandManager.getIslandByPlayer(player.uniqueId) ?: return

        val block = event.block
        if (!island.center.isSameBlock(block.location)) return

        event.isCancelled = true

        if (block.type != Material.AIR) {
            val drops = block.getDrops(player.inventory.itemInMainHand)
            val dropLocation = block.location.add(0.5, 0.5, 0.5)

            for (item in drops) {
                val dropped = block.world.dropItem(dropLocation, item)
                dropped.velocity = dropped.velocity.multiply(0.1)
            }

            // block.breakNaturally(player.inventory.itemInMainHand)
        }

        island.processBlockBreak(player, block)
        IslandManager.markDirty(island.uuid)
        BossbarManager.upgradeBossBar(island)
    }

    private fun handleIslandEntry(player: Player, location: Location) {
        runNextTask {
            val islandAt = IslandManager.getIslandAt(location) ?: return@runNextTask
            IslandManager.playerIsland[player.uniqueId] = islandAt.uuid
            islandAt.onJoin(player)
        }
    }
}