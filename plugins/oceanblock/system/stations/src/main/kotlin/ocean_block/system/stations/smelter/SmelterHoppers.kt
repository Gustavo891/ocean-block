package ocean_block.system.stations.smelter

import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.inventory.Inventory

object SmelterHoppers {

    private val SIDES = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

    fun exchange(block: Block, state: SmelterState): SmelterState {
        var current = state
        inventory(block, BlockFace.UP)?.let { current = pull(it, current) { s, id, amount -> SmelterRules.insertOre(s, id, amount) } }
        SIDES.forEach { face ->
            inventory(block, face)?.let { current = pull(it, current) { s, id, amount -> if (Fuel.ticks(id) != null) SmelterRules.insertFuel(s, id, amount) else s to 0 } }
        }
        inventory(block, BlockFace.DOWN)?.let { current = push(it, current) }
        return current
    }

    private fun inventory(block: Block, face: BlockFace): Inventory? = (block.getRelative(face).state as? Container)?.inventory

    private fun pull(inventory: Inventory, state: SmelterState, insert: (SmelterState, String, Int) -> Pair<SmelterState, Int>): SmelterState {
        inventory.contents.forEachIndexed { slot, stack ->
            val id = SmelterItems.idOf(stack) ?: return@forEachIndexed
            val (next, taken) = insert(state, id, 1)
            if (taken == 0) return@forEachIndexed
            val item = checkNotNull(stack)
            item.amount -= taken
            inventory.setItem(slot, item.takeIf { it.amount > 0 })
            return next
        }
        return state
    }

    private fun push(inventory: Inventory, state: SmelterState): SmelterState {
        val output = state.output ?: return state
        val stack = SmelterItems.stackOf(output, state.outputCount) ?: return state
        val left = inventory.addItem(stack).values.sumOf { it.amount }
        return if (left == 0) state.copy(output = null, outputCount = 0) else state.copy(outputCount = left)
    }
}
