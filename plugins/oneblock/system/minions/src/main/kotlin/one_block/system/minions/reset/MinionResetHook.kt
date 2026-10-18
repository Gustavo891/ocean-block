package one_block.system.minions.reset

import one_block.system.islands.island.Island
import one_block.system.islands.reset.IslandArea
import one_block.system.islands.reset.IslandResetHook
import one_block.system.minions.placed_minion.PlacedMinionManager

object MinionResetHook : IslandResetHook {

    override fun onTerrainReset(island: Island, area: IslandArea) {
        PlacedMinionManager.all()
            .filter { area.contains(it.location) }
            .forEach { PlacedMinionManager.unregister(it.location) }
    }

    override fun describeTerrainReset(island: Island, area: IslandArea): String? {
        val total = PlacedMinionManager.all().count { area.contains(it.location) }
        return if (total == 0) null else "minions: $total"
    }
}
