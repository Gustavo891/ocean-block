package ocean_block.system.islands.phase

import org.bukkit.Material

data class ChestLootEntry(
    val material: Material?,
    val texturaId: String?,
    val weight: Int,
    val min: Int,
    val max: Int,
)

data class ChestLootTable(
    val minRolls: Int,
    val maxRolls: Int,
    val entries: List<ChestLootEntry>,
)
