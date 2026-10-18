package one_block.system.tops.data

import one_block.system.tops.PlayerTop
import org.bukkit.Material

data class TopData(
    val id: String,
    val prettyName: String,
    val iconMaterial: Material,
    val description: List<String>,
    val players: () -> List<PlayerTop>,
) {
}
