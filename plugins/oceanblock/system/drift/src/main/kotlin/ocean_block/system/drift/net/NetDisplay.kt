package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID

object NetDisplay {

    private val TAG = NamespacedKey("oceanblock", "rede_display")
    private val MODEL = NamespacedKey("oneblock", NetBlock.ID)
    private const val ITEM_SCALE = 0.52f
    private const val FLOOR_Y = 0.09
    private const val LAYER_Y = 0.012
    private val SPOTS = listOf(0.36 to 0.36, 0.64 to 0.37, 0.37 to 0.64, 0.63 to 0.63)

    private class Parts(val frame: ItemDisplay, val items: MutableList<ItemDisplay> = mutableListOf(), var shown: List<String> = emptyList()) {
        val isValid: Boolean get() = frame.isValid && items.all { it.isValid }
        fun removeItems() {
            items.forEach { it.remove() }
            items.clear()
            shown = emptyList()
        }
        fun remove() {
            frame.remove()
            removeItems()
        }
    }

    private val displays = mutableMapOf<UUID, Parts>()

    fun ensure(placed: PlacedBlock, block: Block) {
        val current = displays[placed.uuid]
        if (current?.isValid != true) {
            current?.remove()
            displays[placed.uuid] = Parts(spawn(placed, block.location.add(0.5, 0.5, 0.5), modelItem()) {})
        }
        showContents(placed, block)
    }

    fun refresh(placed: PlacedBlock) {
        val block = placed.location.toBukkit()?.block ?: return
        if (displays.containsKey(placed.uuid)) showContents(placed, block)
    }

    private fun showContents(placed: PlacedBlock, block: Block) {
        val parts = displays[placed.uuid] ?: return
        val stored = placed.storageItems().map { it.type.name }
        val shown = NetShowcase.pick(stored, placed.uuid.mostSignificantBits)
        if (shown == parts.shown) return
        parts.removeItems()
        shown.forEachIndexed { index, material ->
            val stack = Material.matchMaterial(material)?.let(::ItemStack) ?: return@forEachIndexed
            val (x, z) = SPOTS[index]
            parts.items += spawn(placed, block.location.add(x, FLOOR_Y + index * LAYER_Y, z), stack) { entity ->
                entity.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.FIXED
                entity.transformation = Transformation(
                    Vector3f(),
                    Quaternionf().rotateY(Math.toRadians(yaw(placed.uuid, index)).toFloat()).rotateX(Math.toRadians(-90.0).toFloat()),
                    Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                    Quaternionf(),
                )
            }
        }
        parts.shown = shown
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

    private fun yaw(placedId: UUID, index: Int): Double = ((placedId.leastSignificantBits ushr (index * 8)) and 0xFF).toDouble() / 255.0 * 360.0

    private fun modelItem(): ItemStack = ItemStack(Material.PAPER).apply { editMeta { it.itemModel = MODEL } }

    private fun spawn(placed: PlacedBlock, at: Location, stack: ItemStack, configure: (ItemDisplay) -> Unit): ItemDisplay =
        at.world.spawn(at, ItemDisplay::class.java) { entity ->
            entity.isPersistent = false
            entity.setItemStack(stack)
            entity.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.NONE
            entity.persistentDataContainer.set(TAG, PersistentDataType.STRING, placed.uuid.toString())
            configure(entity)
        }
}
