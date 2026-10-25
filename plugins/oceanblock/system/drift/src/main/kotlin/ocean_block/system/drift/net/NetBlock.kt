package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.CustomBlock
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.context.BreakContext
import ocean_block.system.custom_items.block.context.PlaceContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block

object NetBlock : CustomBlock() {

    const val ID = "rede"
    const val CAPACITY = 27

    override val id = ID
    override val material = Material.BARRIER
    override val itemModel = NamespacedKey("oneblock", ID)
    override val furnitureFeel = Material.DARK_OAK_PLANKS
    override val displayName = "<white>Rede de Detritos"
    override val lore = listOf(
        "<gray>Coleta detritos que passam",
        "<gray>a até 3 blocos.",
        "<gray>Clique direito para abrir.",
    )
    override val limitPerIsland = 4

    override fun storageCapacity(placed: PlacedBlock): Int = CAPACITY

    override fun onPlace(ctx: PlaceContext) {
        NetDisplay.ensure(ctx.placed, ctx.block)
    }

    override fun onBreak(ctx: BreakContext) {
        NetDisplay.remove(ctx.placed.uuid)
    }

    override fun tick(placed: PlacedBlock) {
        val block = placed.location.toBukkit()?.block ?: return
        NetDisplay.ensure(placed, block)
    }

    override fun adopt(placed: PlacedBlock, block: Block): Boolean {
        if (!NetRules.adopts(block.type.name)) return false
        block.setType(Material.BARRIER, false)
        NetDisplay.ensure(placed, block)
        return true
    }
}
