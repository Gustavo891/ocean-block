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
import org.bukkit.World
import java.io.File
import java.io.FileInputStream

object StarterIsland {

    private const val FOLDER = "templates"
    private const val SCHEMATIC = "island.schem"

    private val schematic: File
        get() = File(File(Instance.dataFolder, FOLDER), SCHEMATIC)

    fun generate(island: Island, onReady: () -> Unit = {}) {
        val area = IslandArea.of(island)
        val center = island.center.toBukkit()

        if (area == null || center == null) {
            Instance.logger.severe("[ilha] Mundo da ilha ${island.uuid} não carregado; ilha inicial pulada.")
            onReady()
            return
        }

        val file = schematic

        if (!file.exists()) {
            Instance.logger.severe("[ilha] '${file.path}' não existe; usando a plataforma de fallback na ilha ${island.uuid}.")
            placeFallback(island)
            onReady()
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(Instance, Runnable {
            val result = runCatching { paste(file, area.world, center.blockX, center.blockZ) }

            Bukkit.getScheduler().runTask(Instance, Runnable {
                result.onFailure {
                    Instance.logger.severe("[ilha] Falha ao colar a ilha ${island.uuid}: ${it.message}; usando a plataforma de fallback.")
                    placeFallback(island)
                }
                onReady()
            })
        })
    }

    private fun paste(file: File, world: World, centerX: Int, centerZ: Int) {
        val format = ClipboardFormats.findByFile(file)
            ?: error("formato de schematic desconhecido: ${file.name}")

        val clipboard = FileInputStream(file).use { input ->
            format.getReader(input).use { it.read() }
        }

        WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world)).use { edit ->
            val operation = ClipboardHolder(clipboard)
                .createPaste(edit)
                .to(BlockVector3.at(centerX, IslandPlacement.PASTE_Y, centerZ))
                .ignoreAirBlocks(true)
                .build()

            Operations.complete(operation)
        }
    }

    fun placeFallback(island: Island) {
        val center = island.center.toBukkit() ?: return
        val world = center.world ?: return

        IslandPlacement.fallbackFootprint(center.blockX, center.blockZ).forEach { (x, z) ->
            world.getBlockAt(x, IslandPlacement.GROUND_Y - 1, z).type = Material.DIRT
            world.getBlockAt(x, IslandPlacement.GROUND_Y, z).type = Material.GRASS_BLOCK
        }
    }
}
