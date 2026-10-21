package ocean_block.system.stations.smelter

import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.UUID

object SmelterDisplay {

    private const val HEIGHT = 1.15
    private const val SCALE = 0.45f

    private val displays = mutableMapOf<UUID, ItemDisplay>()
    private val shown = mutableMapOf<UUID, String>()

    fun update(placedId: UUID, block: Block, input: String?) {
        if (input == null) return remove(placedId)
        val current = displays[placedId]?.takeIf { it.isValid }
        if (current != null && shown[placedId] == input) return
        current?.remove()
        val stack = SmelterItems.stackOf(input, 1) ?: return remove(placedId)
        val location = block.location.add(0.5, HEIGHT, 0.5)
        val display = block.world.spawn(location, ItemDisplay::class.java) { entity ->
            entity.isPersistent = false
            entity.setItemStack(stack)
            entity.transformation = Transformation(Vector3f(), AxisAngle4f(), Vector3f(SCALE, SCALE, SCALE), AxisAngle4f())
        }
        displays[placedId] = display
        shown[placedId] = input
    }

    fun remove(placedId: UUID) {
        displays.remove(placedId)?.remove()
        shown.remove(placedId)
    }

    fun removeAll() {
        displays.values.forEach { it.remove() }
        displays.clear()
        shown.clear()
    }
}
