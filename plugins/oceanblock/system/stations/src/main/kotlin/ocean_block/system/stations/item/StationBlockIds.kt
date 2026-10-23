package ocean_block.system.stations.item

import ocean_block.system.stations.smelter.SmelterTier

object StationBlockIds {

    private val ids: Set<String> = SmelterTier.entries.map { it.blockId }.toSet()

    fun isStation(id: String?): Boolean = id != null && id in ids
}
