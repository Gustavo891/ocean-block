package one_block.system.custom_items.reset

import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.islands.island.Island
import one_block.system.islands.reset.IslandArea
import one_block.system.islands.reset.IslandResetHook

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
