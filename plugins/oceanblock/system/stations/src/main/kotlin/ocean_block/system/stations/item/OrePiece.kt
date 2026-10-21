package ocean_block.system.stations.item

import ocean_block.system.custom_items.item.CustomItem
import org.bukkit.Material

class OrePiece private constructor(
    override val id: String,
    override val material: Material,
    name: String,
    val ingot: String,
) : CustomItem() {

    override val displayName: String = "<white>$name"
    override val lore: List<String> = listOf("<gray>Derreta 4 na Fundição", "<gray>para ganhar um lingote.")
    val label: String = name

    companion object {
        val IRON = OrePiece("pedaco_ferro", Material.RAW_IRON, "Pedaço de ferro", "IRON_INGOT")
        val COPPER = OrePiece("pedaco_cobre", Material.RAW_COPPER, "Pedaço de cobre", "COPPER_INGOT")
        val GOLD = OrePiece("pedaco_ouro", Material.RAW_GOLD, "Pedaço de ouro", "GOLD_INGOT")

        val all: List<OrePiece> = listOf(IRON, COPPER, GOLD)

        fun byId(id: String): OrePiece? = all.firstOrNull { it.id == id }
    }
}
