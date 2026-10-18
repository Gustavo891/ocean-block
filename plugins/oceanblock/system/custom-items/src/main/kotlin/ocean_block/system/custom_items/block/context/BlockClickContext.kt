package ocean_block.system.custom_items.block.context

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.custom_items.block.PlacedBlock
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable

class BlockClickContext(
    val player: Player,
    val block: Block,
    val placed: PlacedBlock,
    val viewFrame: ViewFrame,
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }
}
