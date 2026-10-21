package ocean_block.system.stations.sieve

import ocean_block.system.custom_items.block.PlacedBlock
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.inventory.Inventory

object AutoSieve {

    fun tick(placed: PlacedBlock, tier: SieveTier) {
        val block = placed.location.toBukkit()?.block ?: return
        var state = SieveProgress.fromCounters(placed.counters)
        if (state.input == null) {
            val source = (block.getRelative(BlockFace.UP).state as? Container)?.inventory ?: return
            val input = takeInput(source) ?: return
            state = SieveProgress.load(state, input, tier) ?: return
            SieveActions.save(block, placed, state, tier)
            return
        }
        val target = (block.getRelative(BlockFace.DOWN).state as? Container)?.inventory
        if (state.stepsLeft <= 1 && (target == null || target.firstEmpty() == -1)) return
        state = SieveActions.advance(block, placed, state, tier) { outputs ->
            outputs.forEach { (output, amount) ->
                val stack = SieveDelivery.stackOf(output, amount) ?: return@forEach
                target?.addItem(stack)?.values?.forEach { block.world.dropItemNaturally(block.location.add(0.5, 1.0, 0.5), it) }
            }
        }
        SieveActions.save(block, placed, state, tier)
    }

    private fun takeInput(inventory: Inventory): SieveInput? {
        val slot = inventory.contents.indexOfFirst { stack -> stack != null && SieveInput.of(stack.type.name) != null }
        if (slot < 0) return null
        val stack = inventory.getItem(slot) ?: return null
        val input = SieveInput.of(stack.type.name) ?: return null
        stack.amount -= 1
        inventory.setItem(slot, stack.takeIf { it.amount > 0 })
        return input
    }
}
