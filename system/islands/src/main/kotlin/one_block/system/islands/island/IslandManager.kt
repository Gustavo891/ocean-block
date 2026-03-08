package one_block.system.islands.island

import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.libs.instance.ScheduledExecutor
import one_block.system.islands.IslandsSystem
import one_block.system.islands.bossbar.BossbarManager
import one_block.system.islands.position.Position
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*
import java.util.Locale
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object IslandManager {

    val islandsById = mutableMapOf<UUID, Island>()

    private val islandsByGrid = mutableMapOf<Pair<Int, Int>, Island>()
    private val dirtyIslands = mutableSetOf<UUID>()

    val playerIsland = mutableMapOf<UUID, UUID>()

    private val repository = IslandRepository()

    fun onInit() {
        repository.toList().forEach {
            islandsById[it.uuid] = it
            islandsByGrid[it.position.gridX to it.position.gridZ] = it
            BossbarManager.initBossBar(it)
        }

        ScheduledExecutor.scheduleAtFixedRate({ saveDirtyIslands() }, 30, 30, TimeUnit.SECONDS)
    }


   /*fun createIsland(uuid: UUID, name: String): Island {
        val index = islandsById.size
        val grid = Position.Companion.fromIndex(index)

        val center = SimpleLocation(
            grid.originX,
            101.0,
            grid.originZ,
            world = "world"
        )

        val spawn = SimpleLocation(
            grid.originX + 0.5,
            100.0,
            grid.originZ + 0.5,
            world = "world"
        )

        generateStarterIsland(center)

        val islandId = UUID.randomUUID()

        val island = Island(
            islandId,
            uuid,
            name.lowercase(),
            center,
            grid,
            spawn,
            isPublic = false
        )

        islandsById[islandId] = island
        playerIsland[uuid] = islandId
        islandsByGrid[grid.gridX to grid.gridZ] = island
        markDirty(islandId)

        return island
    }*/

    fun createIsland(player: Player): Island {
        val index = islandsById.size
        val grid = Position.fromIndex(index)

        val center = SimpleLocation(
            grid.originX,
            101.0,
            grid.originZ,
            world = "world"
        )

        val spawn = SimpleLocation(
            grid.originX + 0.5,
            100.0,
            grid.originZ + 0.5,
            world = "world"
        )

        generateStarterIsland(center)

        val islandId = UUID.randomUUID()

        val island = Island(
            islandId,
            player.uniqueId,
            player.name.lowercase(),
            center,
            grid,
            spawn,
            isPublic = false
        )

        islandsById[islandId] = island
        playerIsland[player.uniqueId] = islandId
        islandsByGrid[grid.gridX to grid.gridZ] = island
        markDirty(islandId)

        return island
    }

    fun getIslandByOwnerName(targetName: String): Island? {
        return islandsById.values.find {
            it.ownerName == targetName
        }
    }

    fun getIslandByOwnerUUID(targetUUID: UUID): Island? {
        return islandsById.values.find {
            it.ownerUuid == targetUUID
        }
    }

    fun getIslandById(id: UUID): Island? {
        return islandsById[id]
    }

    fun getIslandAt(location: Location): Island? {
        if (location.world?.name != "world") return null

        val gx = (location.x / Position.GRID_SPACING).roundToInt()
        val gz = (location.z / Position.GRID_SPACING).roundToInt()
        val island = islandsByGrid[gx to gz] ?: return null

        val dist = island.size / 2.0
        val centerX = island.position.originX
        val centerZ = island.position.originZ

        val inside = location.x >= centerX - dist && location.x <= centerX + dist &&
            location.z >= centerZ - dist && location.z <= centerZ + dist

        return if (inside) island else null
    }

    fun getIslandByPlayer(uuid: UUID): Island? {
        val islandId = playerIsland[uuid] ?: return null
        return islandsById[islandId]
    }

    fun markDirty(islandUuid: UUID) {
        dirtyIslands.add(islandUuid)
    }

    private fun saveDirtyIslands() {
        if (dirtyIslands.isEmpty()) return

        val toSave = dirtyIslands.toSet()
        dirtyIslands.clear()

        toSave.forEach { uuid ->
            val island = islandsById[uuid] ?: return@forEach
            repository.update(island)
        }

        Instance.logger.info("[islands] Salvas ${toSave.size} ilhas que sofreram alterações.")
    }

    private fun generateStarterIsland(center: SimpleLocation) {
        val location = center.toBukkit() ?: return
        val world = location.world ?: return

        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ

        world.getBlockAt(x, y - 1, z).type = Material.BEDROCK
        world.getBlockAt(x, y, z).type = Material.DIRT
    }
}