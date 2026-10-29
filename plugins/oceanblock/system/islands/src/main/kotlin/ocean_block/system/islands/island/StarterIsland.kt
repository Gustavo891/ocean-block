package ocean_block.system.islands.island

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockState
import ocean_block.libs.instance.Instance
import ocean_block.system.islands.island.type.IslandBiome
import ocean_block.system.islands.island.type.IslandPalette
import ocean_block.system.islands.island.type.IslandType
import ocean_block.system.islands.reset.IslandArea
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import java.io.File
import java.io.FileInputStream

object StarterIsland {

    private const val FOLDER = "templates"
    private const val SCHEMATIC = "island.schem"
    private const val RAFT_Y = 62

    private fun template(name: String): File = File(File(Instance.dataFolder, FOLDER), name)

    fun generate(island: Island, onReady: () -> Unit = {}) {
        val area = IslandArea.of(island)
        val center = island.center.toBukkit()

        if (area == null || center == null) {
            Instance.logger.severe("[ilha] Mundo da ilha ${island.uuid} não carregado; ilha inicial pulada.")
            onReady()
            return
        }

        val type = island.type()

        if (type == IslandType.JANGADA && !template("${type.id}.schem").exists()) {
            placeRaft(island)
            onReady()
            return
        }

        val own = template("${type.id}.schem")
        val file = if (own.exists()) own else template(SCHEMATIC)
        val swap = !own.exists()

        if (!file.exists()) {
            Instance.logger.severe("[ilha] '${file.path}' não existe; usando a plataforma de fallback na ilha ${island.uuid}.")
            placeFallback(island)
            IslandBiome.paint(island)
            onReady()
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(Instance, Runnable {
            val result = runCatching { paste(file, area.world, center.blockX, center.blockZ, if (swap) type else IslandType.PADRAO) }

            Bukkit.getScheduler().runTask(Instance, Runnable {
                result.onFailure {
                    Instance.logger.severe("[ilha] Falha ao colar a ilha ${island.uuid}: ${it.message}; usando a plataforma de fallback.")
                    placeFallback(island)
                }
                IslandBiome.paint(island)
                onReady()
            })
        })
    }

    private fun paste(file: File, world: World, centerX: Int, centerZ: Int, type: IslandType) {
        val format = ClipboardFormats.findByFile(file)
            ?: error("formato de schematic desconhecido: ${file.name}")

        val clipboard = FileInputStream(file).use { input ->
            format.getReader(input).use { it.read() }
        }

        retheme(clipboard, type)

        WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world)).use { edit ->
            val operation = ClipboardHolder(clipboard)
                .createPaste(edit)
                .to(BlockVector3.at(centerX, IslandPlacement.PASTE_Y, centerZ))
                .ignoreAirBlocks(true)
                .build()

            Operations.complete(operation)
        }
    }

    private fun retheme(clipboard: Clipboard, type: IslandType) {
        if (type == IslandType.PADRAO) return
        val cache = mutableMapOf<String, BlockState>()

        clipboard.region.forEach { position ->
            val original = clipboard.getBlock(position).asString
            val swapped = IslandPalette.swap(type, original)
            if (swapped != original) {
                clipboard.setBlock(position.x(), position.y(), position.z(), cache.getOrPut(swapped) { BlockState.get(swapped) })
            }
        }
    }

    private fun placeRaft(island: Island) {
        val center = island.center.toBukkit() ?: return
        val world = center.world ?: return

        IslandPlacement.fallbackFootprint(center.blockX, center.blockZ).forEach { (x, z) ->
            world.getBlockAt(x, RAFT_Y, z).type = Material.OAK_PLANKS
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
