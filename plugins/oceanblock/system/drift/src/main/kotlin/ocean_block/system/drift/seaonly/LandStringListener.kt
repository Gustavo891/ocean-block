package ocean_block.system.drift.seaonly

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.event.entity.PiglinBarterEvent
import org.bukkit.event.world.LootGenerateEvent

class LandStringListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onDeath(event: EntityDeathEvent) {
        if (!LandStringRules.stripsMobDrops(event.entityType)) return
        event.drops.removeIf { LandStringRules.isSeaOnly(it.type) }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockDrop(event: BlockDropItemEvent) {
        if (!LandStringRules.stripsBlockDrops(event.blockState.type)) return
        event.items.removeIf { LandStringRules.isSeaOnly(it.itemStack.type) }
    }

    @EventHandler(ignoreCancelled = true)
    fun onLoot(event: LootGenerateEvent) {
        if (!LandStringRules.stripsLoot(event.lootTable.key.key)) return
        event.setLoot(LandStringRules.withoutSeaOnly(event.loot) { it.type })
    }

    @EventHandler(ignoreCancelled = true)
    fun onBarter(event: PiglinBarterEvent) {
        event.outcome.removeIf { LandStringRules.isSeaOnly(it.type) }
    }

    @EventHandler(ignoreCancelled = true)
    fun onGift(event: EntityDropItemEvent) {
        if (LandStringRules.blocksGift(event.entityType, event.itemDrop.itemStack.type)) {
            event.isCancelled = true
        }
    }
}
