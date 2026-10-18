package one_block.system.spawners.manager

import net.kyori.adventure.text.Component
import one_block.libs.formatter.Mini
import one_block.system.spawners.SpawnersSystem
import one_block.system.spawners.models.PlacedSpawner
import one_block.system.spawners.models.SpawnerType
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object SpawnerDisplay {

    private const val SEARCH_RADIUS = 1.0

    private val displayMap = mutableMapOf<UUID, TextDisplay>()

    private fun location(placed: PlacedSpawner): Location? =
        placed.location.toBukkit()?.add(0.5, 1.5, 0.5)

    private fun ownerOf(display: TextDisplay): UUID? {
        val raw = display.persistentDataContainer
            .get(SpawnersSystem.OWNER_SPAWNER, PersistentDataType.STRING)
            ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    private fun find(placed: PlacedSpawner): TextDisplay? {
        displayMap[placed.uuid]?.let { cached ->
            if (cached.isValid) return cached
            displayMap.remove(placed.uuid)
        }

        val location = location(placed) ?: return null
        val world = location.world ?: return null
        if (!world.isChunkLoaded(location.blockX shr 4, location.blockZ shr 4)) return null

        return world.getNearbyEntities(location, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
            .filterIsInstance<TextDisplay>()
            .firstOrNull { ownerOf(it) == placed.uuid }
            ?.also { displayMap[placed.uuid] = it }
    }

    fun reconcile(placed: PlacedSpawner, type: SpawnerType) {
        val existing = find(placed)
        if (existing != null) {
            existing.text(textOf(placed, type))
            return
        }

        val location = location(placed) ?: return
        val world = location.world ?: return

        world.spawn(location, TextDisplay::class.java) { display ->
            display.isPersistent = false
            display.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            display.billboard = Billboard.VERTICAL
            display.isShadowed = true
            display.persistentDataContainer.set(
                SpawnersSystem.OWNER_SPAWNER,
                PersistentDataType.STRING,
                placed.uuid.toString(),
            )
            displayMap[placed.uuid] = display
            display.text(textOf(placed, type))
        }
    }

    fun remove(placed: PlacedSpawner) {
        find(placed)?.remove()
        displayMap.remove(placed.uuid)
    }

    private fun textOf(placed: PlacedSpawner, type: SpawnerType) : Component {
        val status = if(placed.enabled) "<green>■ Ativado" else "<red>■ Desativado"
        return Mini.deserialize(
            "<head:${type.headOwner}> <white>Gerador de <gray>${type.displayName}" +
            "\n" +
            "<white>Status: $status\n" +
            "<white>Quantidade: <gray>${placed.stackSize}"
        )
    }
}
