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

    open val texturaBlock: Boolean = true

    override fun buildItem(placed: PlacedBlock?, amount: Int): ItemStack =
        super.buildItem(placed, amount).also { if (texturaBlock) BlockApi.markItem(it, id) }

    final override fun tick(placed: PlacedBlock) {
        restoreAppearance(placed)
        work(placed)
    }

    protected abstract fun work(placed: PlacedBlock)

    protected open fun appearanceId(placed: PlacedBlock): String = id

    protected fun restoreAppearance(placed: PlacedBlock) {
        val block = placed.location.toBukkit()?.block ?: return
        val expected = BlockApi.blockData(appearanceId(placed)) ?: return
        if (block.type == Material.NOTE_BLOCK && block.blockData != expected) block.setBlockData(expected, false)
    }

    companion object {
        const val TICKS = 10
        private const val NAMESPACE = "oneblock"
    }
}
