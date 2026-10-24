package ocean_block.system.machines

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.custom_items.block.context.BlockClickContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class MachineInteractListener(
    private val viewFrame: ViewFrame,
    private val machines: Map<String, MachineBlock>,
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (player.isSneaking) return
        val block = event.clickedBlock ?: return
        val placed = PlacedBlockManager.get(block) ?: return
        val machine = machines[placed.blockId] ?: return

        event.isCancelled = true
        if (!MachineItems.canUse(player, placed)) {
            player.sendRichMessage("<red>Você não tem acesso a este bloco.")
            return
        }
        machine.onRightClick(BlockClickContext(player, block, placed, viewFrame, event))
    }
}
