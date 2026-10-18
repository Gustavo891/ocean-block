package ocean_block.system.islands.listener

import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandSetting
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityChangeBlockEvent

class IslandSettingListener : Listener {

    companion object {
        val NATURAL_SPAWN_REASONS = setOf(
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.PATROL,
            CreatureSpawnEvent.SpawnReason.REINFORCEMENTS,
        )

        val FIRE_SPREAD_CAUSES = setOf(
            BlockIgniteEvent.IgniteCause.SPREAD,
            BlockIgniteEvent.IgniteCause.LAVA,
            BlockIgniteEvent.IgniteCause.LIGHTNING,
        )
    }

    @EventHandler(ignoreCancelled = true)
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (event.spawnReason !in NATURAL_SPAWN_REASONS) return
        if (!isDisabled(event.location, IslandSetting.NaturalMobSpawn)) return

        event.isCancelled = true
    }

    @EventHandler
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        if (!isDisabled(event.block.location, IslandSetting.Redstone)) return

        event.newCurrent = 0
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (!isDisabled(event.block.location, IslandSetting.Redstone)) return

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (!isDisabled(event.block.location, IslandSetting.Redstone)) return

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onLeavesDecay(event: LeavesDecayEvent) {
        if (!isDisabled(event.block.location, IslandSetting.LeafDecay)) return

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        if (!isDisabled(event.toBlock.location, IslandSetting.LiquidFlow)) return

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        if (event.cause !in FIRE_SPREAD_CAUSES) return
        if (!isDisabled(event.block.location, IslandSetting.FireSpread)) return

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        if (!isDisabled(event.block.location, IslandSetting.FireSpread)) return

        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        val entity = event.entity
        if (entity !is LivingEntity || entity is Player) return
        if (!isDisabled(event.block.location, IslandSetting.MobGriefing)) return

        event.isCancelled = true
    }

    private fun isDisabled(location: Location, setting: IslandSetting): Boolean {
        val island = IslandManager.getIslandAt(location) ?: return false
        if (island.settings == null) return false

        return !island.isSettingEnabled(setting)
    }
}
