package ocean_block.system.machines.energy

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.machines.MachineIds
import java.util.UUID

object EnergyIndex {

    @Volatile
    private var byIsland: Map<UUID, List<UUID>> = emptyMap()

    fun rebuild() {
        byIsland = PlacedBlockManager.all()
            .filter { it.blockId in MachineIds.sources }
            .mapNotNull { placed -> islandOf(placed)?.let { it to placed.uuid } }
            .groupBy({ it.first }, { it.second })
    }

    fun sourcesNear(consumer: PlacedBlock): List<Pair<PlacedBlock, Double>> {
        val island = islandOf(consumer) ?: return emptyList()
        val center = consumer.location.toBukkit() ?: return emptyList()
        return byIsland[island].orEmpty()
            .mapNotNull { PlacedBlockManager.get(it) }
            .filter { PlacedBlockManager.isRegistered(it) && it.uuid != consumer.uuid }
            .mapNotNull { source ->
                val location = source.location.toBukkit()?.takeIf { it.world == center.world } ?: return@mapNotNull null
                source to location.distance(center)
            }
    }

    private fun islandOf(placed: PlacedBlock): UUID? =
        placed.location.toBukkit()?.let(IslandManager::getIslandAt)?.uuid
}

object EnergyNetwork {

    fun stored(placed: PlacedBlock): Long = placed.counters[EnergyRules.KEY] ?: 0

    fun available(consumer: PlacedBlock): Long =
        EnergyIndex.sourcesNear(consumer).filter { it.second <= EnergyRules.RADIUS }.sumOf { stored(it.first) }

    fun draw(consumer: PlacedBlock, needed: Long): Boolean {
        val near = EnergyIndex.sourcesNear(consumer)
        val plan = EnergyRules.plan(needed, near.map { (source, distance) -> EnergySource(source.uuid, distance, stored(source)) })
        plan?.forEach { (id, take) ->
            val source = PlacedBlockManager.get(id) ?: return@forEach
            source.counters[EnergyRules.KEY] = stored(source) - take
            PlacedBlockManager.markDirty(source)
        }
        setNoPower(consumer, plan == null)
        return plan != null
    }

    fun hasNoPower(consumer: PlacedBlock): Boolean = consumer.counters[MachineIds.NO_POWER_KEY] == 1L

    private fun setNoPower(consumer: PlacedBlock, noPower: Boolean) {
        val value = if (noPower) 1L else 0L
        if ((consumer.counters[MachineIds.NO_POWER_KEY] ?: 0L) == value) return
        consumer.counters[MachineIds.NO_POWER_KEY] = value
        PlacedBlockManager.markDirty(consumer)
    }
}
