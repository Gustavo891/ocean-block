package ocean_block.system.machines

import ocean_block.system.custom_items.block.CustomBlock
import ocean_block.system.custom_items.block.PlacedBlock
import one_block.textura.api.BlockApi
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

abstract class MachineBlock : CustomBlock() {

    override val material: Material = Material.NOTE_BLOCK

    override val itemModel: NamespacedKey?
        get() = NamespacedKey(NAMESPACE, id)

    override fun tickInterval(placed: PlacedBlock): Long = TICKS.toLong()

    override fun buildItem(placed: PlacedBlock?, amount: Int): ItemStack =
        super.buildItem(placed, amount).also { BlockApi.markItem(it, id) }

    companion object {
        const val TICKS = 10
        private const val NAMESPACE = "oneblock"
    }
}
