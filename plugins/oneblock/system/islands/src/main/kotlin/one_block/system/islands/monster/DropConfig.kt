package one_block.system.islands.monster

import org.bukkit.Material

data class DropConfig(
    val type: DropType,
    val material: Material?,
    val amount: Int?,
    val command: String?,
    val chance: Double,
)

enum class DropType {
    ITEM,
    COMMAND
}
