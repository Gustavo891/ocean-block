package ocean_block.system.islands.island

import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import ocean_block.system.islands.phase.PhaseManager
import ocean_block.system.islands.position.Position
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object IslandManager {

    private const val RAFT_WORLD = "world"

    val islandsById = mutableMapOf<UUID, Island>()

    private val islandsByGrid = mutableMapOf<Pair<Int, Int>, Island>()
    private val islandsByOwnerUuid = mutableMapOf<UUID, Island>()
    private val islandsByOwnerName = mutableMapOf<String, Island>()
    private val dirtyIslands = mutableSetOf<UUID>()

    val playerIsland = mutableMapOf<UUID, UUID>()

    private val repository = IslandRepository()

    var loaded = false
        private set

    fun onInit() {
        repository.migrateMissingFields()

        val islands = runCatching { repository.toList() }
            .onFailure {
                Instance.logger.severe(
                    "[islands] FALHA AO CARREGAR AS ILHAS: ${it.message}. " +
                        "Nenhuma ilha será criada até o servidor reiniciar com o problema resolvido."
                )
            }
            .getOrNull() ?: return

        islands.forEach {
            index(it)

            val key = it.position.gridX to it.position.gridZ
            val occupant = islandsByGrid[key]

            if (occupant != null) {
                Instance.logger.severe(
                    "[islands] COLISÃO DE GRID em (${key.first}, ${key.second}): " +
                        "'${occupant.ownerName}' (${occupant.uuid}) e '${it.ownerName}' (${it.uuid}). " +
                        "Mantendo '${occupant.ownerName}'; mova a outra para um índice livre no banco."
                )
                return@forEach
            }

            islandsByGrid[key] = it
        }

        loaded = true
        Instance.logger.info("[islands] ${islandsById.size} ilha(s) carregada(s).")

        migrateUnknownPhases()

        ScheduledExecutor.scheduleAtFixedRate({ saveDirtyIslands() }, 30, 30, TimeUnit.SECONDS)
    }

    private fun migrateUnknownPhases() {
        val fallback = PhaseManager.phases().minByOrNull { it.priority } ?: return

        val orfas = islandsById.values.filter { PhaseManager.get(it.currentPhaseId) == null }
        if (orfas.isEmpty()) return

        orfas.forEach { island ->
            Instance.logger.warning(
                "[islands] ilha de '${island.ownerName}' apontava para a fase inexistente " +
                    "'${island.currentPhaseId}'; movida para '${fallback.id}'."
            )

            island.currentPhaseId = fallback.id
            markDirty(island.uuid)
        }

        Instance.logger.warning(
            "[islands] ${orfas.size} ilha(s) reancorada(s) em '${fallback.id}'."
        )
    }


    fun createIsland(player: Player): Island? {
        if (!loaded) {
            Instance.logger.severe(
                "[islands] Criação de ilha para ${player.name} recusada: as ilhas não foram carregadas."
            )
            return null
        }

        if (Bukkit.getWorld(RAFT_WORLD) == null) {
            Instance.logger.severe(
                "[islands] Criação de ilha para ${player.name} recusada: mundo $RAFT_WORLD não carregado."
            )
            return null
        }

        val grid = Position.fromIndex(nextFreeIndex())

        if (islandsByGrid[grid.gridX to grid.gridZ] != null) {
            Instance.logger.severe(
                "[islands] Criação de ilha para ${player.name} recusada: " +
                    "o grid (${grid.gridX}, ${grid.gridZ}) já está ocupado."
            )
            return null
        }

        val center = RaftPlacement.center(grid, RAFT_WORLD)
        val spawn = RaftPlacement.spawn(grid, RAFT_WORLD)

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

        index(island)
        playerIsland[player.uniqueId] = islandId
        islandsByGrid[grid.gridX to grid.gridZ] = island
        markDirty(islandId)

        StarterIsland.generate(island)

        return island
    }

    private fun index(island: Island) {
        islandsById[island.uuid] = island
        islandsByOwnerUuid[island.ownerUuid] = island
        islandsByOwnerName[island.ownerName.lowercase()] = island
    }

    private fun nextFreeIndex(): Int {
        var index = maxOf((islandsById.values.maxOfOrNull { it.position.index } ?: 0) + 1, 1)

        while (islandsByGrid[Position.fromIndex(index).let { it.gridX to it.gridZ }] != null) {
            index++
        }

        return index
    }

    fun getIslandByOwnerName(targetName: String): Island? = islandsByOwnerName[targetName.lowercase()]

    fun getIslandByOwnerUUID(targetUUID: UUID): Island? = islandsByOwnerUuid[targetUUID]

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

    fun onDisable() {
        saveDirtyIslands()
    }

    private fun saveDirtyIslands() {
        if (dirtyIslands.isEmpty()) return

        val toSave = dirtyIslands.toSet()
        val islands = toSave.mapNotNull { islandsById[it] }

        val saved = runCatching { repository.updateAll(islands) }
        if (saved.isFailure) {
            Instance.logger.severe(
                "[islands] Falha ao salvar ${islands.size} ilha(s): ${saved.exceptionOrNull()?.message}. " +
                    "Elas seguem marcadas para a proxima tentativa."
            )
            return
        }

        dirtyIslands.removeAll(toSave)
        Instance.logger.info("[islands] Salvas ${islands.size} ilhas que sofreram alterações.")
    }
}
