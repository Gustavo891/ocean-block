package one_block.system.spawners.models

import org.bukkit.Material

data class LootEntry(
    val material: Material,
    val min: Int,
    val max: Int,
    val chance: Double,
)

