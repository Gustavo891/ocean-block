package ocean_block.system.spawn.hologram

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.libs.formatter.Mini
import ocean_block.libs.instance.Instance
import ocean_block.system.spawn.spawn.SpawnManager
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

object SpawnHolograms {

    private val TAG: NamespacedKey by lazy { NamespacedKey(Instance, "spawn_hologram") }

    private data class Definition(
        val location: SimpleLocation,
        val lines: List<String>,
        val scale: Float,
    )

    fun spawnAll() {
        clear()
        definitions().forEach { spawn(it) }
    }

    fun despawnAll() = clear()

    private fun definitions(): List<Definition> {
        val world = SpawnManager.location()?.world ?: return emptyList()

        return listOf(
            Definition(
                location = SimpleLocation(world = world, x = 0.5, y = 62.0, z = -6.5),
                lines = listOf("<dark_gray>(/ilha)", "<gradient:#ff6600:#ffc66b><b>TELEPORTE PARA", "<gradient:#ff6600:#ffc66b><b>SUA ILHA"),
                scale = 2.5f,
            ),
        )
    }

    private fun spawn(definition: Definition) {
        val location = definition.location.toBukkit() ?: return
        val text = Component.join(
            JoinConfiguration.newlines(),
            definition.lines.map(Mini::deserialize),
        )

        location.chunk.isForceLoaded = true

        location.world.spawn(location, TextDisplay::class.java) { display ->
            display.text(text)
            display.billboard = Display.Billboard.VERTICAL
            display.alignment = TextDisplay.TextAlignment.CENTER
            display.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            display.isShadowed = true
            display.isPersistent = true
            display.transformation = Transformation(
                Vector3f(),
                AxisAngle4f(),
                Vector3f(definition.scale, definition.scale, definition.scale),
                AxisAngle4f(),
            )

            display.persistentDataContainer.set(TAG, PersistentDataType.BYTE, 1)
        }
    }

    private fun clear() {
        for (world in Bukkit.getWorlds()) {
            world.entities
                .filter { it.persistentDataContainer.has(TAG, PersistentDataType.BYTE) }
                .forEach { it.remove() }
        }
    }
}
