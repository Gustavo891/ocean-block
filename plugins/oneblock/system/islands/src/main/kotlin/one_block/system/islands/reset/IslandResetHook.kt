package one_block.system.islands.reset

import one_block.system.islands.island.Island

interface IslandResetHook {

    fun onTerrainReset(island: Island, area: IslandArea)

    fun describeTerrainReset(island: Island, area: IslandArea): String? = null
}
