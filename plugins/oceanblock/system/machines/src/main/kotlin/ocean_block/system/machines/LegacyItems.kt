package ocean_block.system.machines

import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.custom_items.CustomIds
import ocean_block.system.custom_items.block.PlacedBlockCodec
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import java.util.UUID

object LegacyItems {

    fun isOutdated(id: String, itemMaterial: String, blockMaterial: String): Boolean =
        id in MachineIds.blocks && itemMaterial != blockMaterial
}

class LegacyItemListener(private val machines: Map<String, MachineBlock>) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) = upgrade(event.player.inventory)

    @EventHandler
    fun onOpen(event: InventoryOpenEvent) {
        upgrade(event.inventory)
        upgrade(event.player.inventory)
    }

    private fun upgrade(inventory: Inventory) {
        inventory.contents.forEachIndexed { slot, item ->
            if (item == null) return@forEachIndexed
            val id = CustomIds.of(item) ?: return@forEachIndexed
            val machine = machines[id] ?: return@forEachIndexed
            if (!LegacyItems.isOutdated(id, item.type.name, machine.material.name)) return@forEachIndexed
            val placed = PlacedBlockCodec.read(item, id, NOWHERE, NOBODY)
            inventory.setItem(slot, machine.buildItem(placed, item.amount))
        }
    }

    private companion object {
        val NOWHERE = SimpleLocation(0.0, 0.0, 0.0)
        val NOBODY = UUID(0, 0)
    }
}
