package ocean_block.system.stations.item

import ocean_block.system.stations.sieve.SieveTier
import ocean_block.system.stations.smelter.SmelterTier

object StationBlockIds {

    private val ids: Set<String> = SieveTier.entries.map { it.blockId }.toSet() + SmelterTier.entries.map { it.blockId }

    fun isStation(id: String?): Boolean = id != null && id in ids
}
