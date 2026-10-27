package ocean_block.system.drift.config

import org.bukkit.Material

data class DebrisType(
    val id: String,
    val weight: Int,
    val display: Material,
    val scale: Float,
    val drops: List<DropEntry>,
    val sink: Double = 0.0,
    val showsDrop: Boolean = false,
)
