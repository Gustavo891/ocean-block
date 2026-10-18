package ocean_block.system.islands.island

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import ocean_block.libs.instance.Instance
import ocean_block.system.islands.reset.IslandArea
import org.bukkit.Bukkit
import org.bukkit.Material
import java.io.File
import java.io.FileInputStream

object StarterIsland {

    private const val FOLDER = "templates"
    private const val SCHEMATIC = "raft.schem"

    private const val PASTE_Y_OFFSET = 1

    private val schematic: File
        get() = File(File(Instance.dataFolder, FOLDER), SCHEMATIC)

    fun generate(island: Island) {
        val area = IslandArea.of(island)
        val center = island.center.toBukkit()

        if (area == null || center == null) {
            Instance.logger.severe("[raft] Mundo da ilha ${island.uuid} não carregado; raft inicial pulado.")
            return
        }

        val file = schematic

        if (!file.exists()) {
            Instance.logger.severe("[raft] '${file.path}' não existe; usando a plataforma de fallback na ilha ${island.uuid}.")
            placeFallback(island)
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(Instance, Runnable {
            runCatching {
                val format = ClipboardFormats.findByFile(file)
                    ?: error("formato de schematic desconhecido: ${file.name}")

                val clipboard = FileInputStream(file).use { input ->
                    format.getReader(input).use { it.read() }
                }

                val weWorld = BukkitAdapter.adapt(area.world)

                WorldEdit.getInstance().newEditSession(weWorld).use { edit ->
                    val operation = ClipboardHolder(clipboard)
                        .createPaste(edit)
                        .to(BlockVector3.at(center.blockX, center.blockY + PASTE_Y_OFFSET, center.blockZ))
                        .ignoreAirBlocks(true)
                        .build()

                    Operations.complete(operation)
                }
            }.onFailure {
                Instance.logger.severe("[raft] Falha ao colar o raft da ilha ${island.uuid}: ${it.message}; usando a plataforma de fallback.")
                Bukkit.getScheduler().runTask(Instance, Runnable { placeFallback(island) })
            }
        })
    }

    fun placeFallback(island: Island) {
        val center = island.center.toBukkit() ?: return
        val world = center.world ?: return

        RaftPlacement.fallbackFootprint(center.blockX, center.blockZ).forEach { (x, z) ->
            world.getBlockAt(x, RaftPlacement.SURFACE_Y, z).type = Material.OAK_PLANKS
        }
    }
}
