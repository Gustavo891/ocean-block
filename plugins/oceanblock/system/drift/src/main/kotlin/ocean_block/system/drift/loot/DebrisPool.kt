package ocean_block.system.drift.loot

import ocean_block.system.drift.config.DebrisType
import ocean_block.system.islands.island.Island

fun interface DebrisPool {
    fun typesFor(island: Island): List<DebrisType>
}

class ConfigDebrisPool(private val types: List<DebrisType>) : DebrisPool {
    override fun typesFor(island: Island): List<DebrisType> = types
}
