package ocean_block.system.custom_items.reset

import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.reset.IslandArea
import ocean_block.system.islands.reset.IslandResetHook

object CustomBlockResetHook : IslandResetHook {

    override fun onTerrainReset(island: Island, area: IslandArea) {
        PlacedBlockManager.all()
            .filter { area.contains(it.location) }
            .forEach { PlacedBlockManager.unregister(it.location) }
    }

    override fun describeTerrainReset(island: Island, area: IslandArea): String? {
        val total = PlacedBlockManager.all().count { area.contains(it.location) }
        return if (total == 0) null else "blocos custom: $total"
    }
}
