package one_block.system.spawn.listener

import one_block.system.spawn.build_mode.BuildMode
import one_block.system.spawn.spawn.SpawnManager
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.weather.WeatherChangeEvent

class SpawnProtectionListener : Listener {

    private companion object {
        const val DAMAGE_BYPASS = "oneblock.bypass.spawn.damage"
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) =
        denyBuild(event, event.player, SpawnManager.isSpawnWorld(event.block.world))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) =
        denyBuild(event, event.player, SpawnManager.isSpawnWorld(event.block.world))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (!SpawnManager.isSpawnWorld(block.world)) return
        if (BuildMode.canBuild(event.player)) return

        event.setUseInteractedBlock(Event.Result.DENY)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) =
        denyBuild(event, event.player, SpawnManager.isAtSpawn(event.player))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketFill(event: PlayerBucketFillEvent) =
        denyBuild(event, event.player, SpawnManager.isAtSpawn(event.player))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onArmorStand(event: PlayerArmorStandManipulateEvent) =
        denyBuild(event, event.player, SpawnManager.isAtSpawn(event.player))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingBreak(event: HangingBreakByEntityEvent) {
        if (!SpawnManager.isSpawnWorld(event.entity.world)) return
        if (BuildMode.canBuild(event.remover as? Player)) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) =
        denyBuild(event, event.player, SpawnManager.isSpawnWorld(event.entity.world))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onIgnite(event: BlockIgniteEvent) =
        denyBuild(event, event.player, SpawnManager.isSpawnWorld(event.block.world))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onSpread(event: BlockSpreadEvent) = denyWorld(event, event.block.world)

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBurn(event: BlockBurnEvent) = denyWorld(event, event.block.world)

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFromTo(event: BlockFromToEvent) = denyWorld(event, event.block.world)

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onLeavesDecay(event: LeavesDecayEvent) = denyWorld(event, event.block.world)

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (!SpawnManager.isSpawnWorld(event.block.world)) return
        if (BuildMode.canBuild(event.entity as? Player)) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) = denyWorld(event, event.location.world)

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) = denyWorld(event, event.block.world)

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onWeatherChange(event: WeatherChangeEvent) {
        if (!SpawnManager.isSpawnWorld(event.world)) return
        if (!event.toWeatherState()) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (!SpawnManager.isSpawnWorld(event.location.world)) return
        if (event.spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) =
        deny(event, event.player, SpawnManager.isAtSpawn(event.player))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onItemDamage(event: PlayerItemDamageEvent) =
        deny(event, event.player, SpawnManager.isAtSpawn(event.player))

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        if (!SpawnManager.isSpawnWorld(event.entity.world)) return

        val attacker = event.damageSource.causingEntity as? Player
        if (attacker != null && attacker.hasPermission(DAMAGE_BYPASS)) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHunger(event: FoodLevelChangeEvent) {
        if (!SpawnManager.isSpawnWorld(event.entity.world)) return
        if (event.foodLevel >= event.entity.foodLevel) return

        event.isCancelled = true
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) = BuildMode.disable(event.player.uniqueId)

    private fun denyWorld(event: Cancellable, world: org.bukkit.World?) {
        if (!SpawnManager.isSpawnWorld(world)) return
        event.isCancelled = true
    }

    private fun denyBuild(event: Cancellable, player: Player?, inSpawn: Boolean) {
        if (!inSpawn) return
        if (BuildMode.canBuild(player)) return

        event.isCancelled = true
    }

    private fun deny(event: Cancellable, player: Player?, inSpawn: Boolean) {
        if (!inSpawn) return
        if (player != null && player.hasPermission(BuildMode.PERMISSION)) return

        event.isCancelled = true
    }
}
