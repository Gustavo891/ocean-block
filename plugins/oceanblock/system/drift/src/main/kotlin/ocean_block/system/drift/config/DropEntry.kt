package ocean_block.system.drift.config

import org.bukkit.Material

data class DropEntry(val material: Material, val min: Int, val max: Int, val chance: Double)
