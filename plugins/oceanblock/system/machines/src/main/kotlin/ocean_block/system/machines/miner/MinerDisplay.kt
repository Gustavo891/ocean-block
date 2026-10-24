package ocean_block.system.machines.miner

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.UUID

object MinerDisplay {

    private val TAG = NamespacedKey("oceanblock", "maquina_display")
    private val CASING_MODEL = NamespacedKey("oneblock", "minerador_carcaca")
    private val DRILL_MODEL = NamespacedKey("oneblock", "minerador_broca")

    private class Parts(val casing: ItemDisplay, val drill: ItemDisplay, var angle: Float = 0f) {
        val isValid: Boolean get() = casing.isValid && drill.isValid
        fun remove() {
            casing.remove()
            drill.remove()
        }
    }

    private val displays = mutableMapOf<UUID, Parts>()

    fun ensure(placed: PlacedBlock, block: Block) {
        if (displays[placed.uuid]?.isValid == true) return
        displays.remove(placed.uuid)?.remove()
        displays[placed.uuid] = Parts(spawn(placed, block, CASING_MODEL), spawn(placed, block, DRILL_MODEL))
    }

    fun spin(placed: PlacedBlock, ticks: Int) {
        val parts = displays[placed.uuid]?.takeIf { it.isValid } ?: return
        parts.angle = MinerRules.nextSpin(parts.angle)
        val radians = Math.toRadians(parts.angle.toDouble()).toFloat()
        parts.drill.interpolationDelay = 0
        parts.drill.interpolationDuration = ticks
        parts.drill.transformation = Transformation(
            Vector3f(),
            AxisAngle4f(radians, 0f, 1f, 0f),
            Vector3f(1f, 1f, 1f),
            AxisAngle4f(),
        )
    }

    fun remove(placedId: UUID) {
        displays.remove(placedId)?.remove()
    }

    fun cleanup() {
        displays.entries.removeIf { (placedId, parts) ->
            val placed = PlacedBlockManager.get(placedId)
            val gone = placed == null || !PlacedBlockManager.isRegistered(placed) || !parts.isValid
            if (gone) parts.remove()
            gone
        }
    }

    fun removeAll() {
        displays.values.forEach { it.remove() }
        displays.clear()
    }

    private fun spawn(placed: PlacedBlock, block: Block, model: NamespacedKey): ItemDisplay =
        block.world.spawn(block.location.add(0.5, 0.5, 0.5), ItemDisplay::class.java) { entity ->
            entity.isPersistent = false
            entity.setItemStack(ItemStack(Material.PAPER).apply { editMeta { it.itemModel = model } })
            entity.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
            entity.persistentDataContainer.set(TAG, PersistentDataType.STRING, placed.uuid.toString())
        }
}
