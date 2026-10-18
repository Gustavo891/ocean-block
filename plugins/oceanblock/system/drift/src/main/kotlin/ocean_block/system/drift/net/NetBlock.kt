package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.CustomBlock
import ocean_block.system.custom_items.block.PlacedBlock
import org.bukkit.Material

object NetBlock : CustomBlock() {

    const val ID = "rede"
    const val CAPACITY = 27

    override val id = ID
    override val material = Material.LOOM
    override val displayName = "<white>Rede de Detritos"
    override val lore = listOf(
        "<gray>Coleta detritos que passam",
        "<gray>a até 3 blocos.",
        "<gray>Clique direito para abrir.",
    )
    override val limitPerIsland = 4

    override fun storageCapacity(placed: PlacedBlock): Int = CAPACITY
}
