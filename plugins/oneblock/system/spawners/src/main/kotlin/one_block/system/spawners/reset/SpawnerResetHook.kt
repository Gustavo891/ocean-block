package one_block.system.spawners.reset

import one_block.system.islands.island.Island
import one_block.system.islands.reset.IslandArea
import one_block.system.islands.reset.IslandResetHook
import one_block.system.spawners.manager.PlacedSpawnerManager

object SpawnerResetHook : IslandResetHook {

    override fun onTerrainReset(island: Island, area: IslandArea) {
        PlacedSpawnerManager.all()
            .filter { area.contains(it.location) }
            .forEach { PlacedSpawnerManager.unregister(it.location) }
    }

    override fun describeTerrainReset(island: Island, area: IslandArea): String? {
        val total = PlacedSpawnerManager.all().count { area.contains(it.location) }
        return if (total == 0) null else "spawners: $total"
    }
}
