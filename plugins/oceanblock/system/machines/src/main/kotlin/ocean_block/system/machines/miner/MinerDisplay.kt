package ocean_block.system.machines.miner

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.MachineIds
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object MinerDisplay {

    private val TAG = NamespacedKey("oceanblock", "maquina_display")
    private val MODEL = NamespacedKey("oneblock", MachineIds.MINER)

    private val displays = mutableMapOf<UUID, ItemDisplay>()

    fun ensure(placed: PlacedBlock, block: Block) {
        if (displays[placed.uuid]?.isValid == true) return
        val display = block.world.spawn(block.location.add(0.5, 0.5, 0.5), ItemDisplay::class.java) { entity ->
            entity.isPersistent = false
            entity.setItemStack(modelItem())
            entity.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
            entity.persistentDataContainer.set(TAG, PersistentDataType.STRING, placed.uuid.toString())
        }
        displays[placed.uuid] = display
    }

    fun remove(placedId: UUID) {
        displays.remove(placedId)?.remove()
    }

    fun cleanup() {
        displays.entries.removeIf { (placedId, display) ->
            val placed = PlacedBlockManager.get(placedId)
            val gone = placed == null || !PlacedBlockManager.isRegistered(placed) || !display.isValid
            if (gone) display.remove()
            gone
        }
    }

    fun removeAll() {
        displays.values.forEach { it.remove() }
        displays.clear()
    }

    private fun modelItem(): ItemStack = ItemStack(Material.PAPER).apply {
        editMeta { it.itemModel = MODEL }
    }
}
