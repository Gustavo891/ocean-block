package one_block.system.islands.island

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import one_block.libs.instance.Instance
import one_block.system.islands.reset.IslandArea
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.block.Biome
import java.io.File
import java.io.FileInputStream

object StarterIsland {

    private const val FOLDER = "templates"
    private const val SCHEMATIC = "forest.schem"
    private const val BIOME = "forest"

    private const val BIOME_STEP = 4

    private const val PASTE_Y_OFFSET = 0

    private val schematic: File
        get() = File(File(Instance.dataFolder, FOLDER), SCHEMATIC)

    private val biome: Biome?
        get() = Registry.BIOME.get(NamespacedKey.minecraft(BIOME))

    fun generate(island: Island) {
        placeFloor(island)

        val area = IslandArea.of(island)
        val center = island.center.toBukkit()

        if (area == null || center == null) {
            Instance.logger.severe("[ilha] Mundo da ilha ${island.uuid} não carregado; terreno inicial pulado.")
            return
        }

        val file = schematic

        if (!file.exists()) {
            Instance.logger.severe("[ilha] '${file.path}' não existe; a ilha nasceu só com o bloco central.")
            return
        }

        val biomeType = biome?.let { BukkitAdapter.adapt(it) }

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
                        .ignoreAirBlocks(false)
                        .build()

                    Operations.complete(operation)

                    if (biomeType == null) return@use

                    var x = area.minX
                    while (x <= area.maxX) {
                        var z = area.minZ
                        while (z <= area.maxZ) {
                            var y = area.minY
                            while (y <= area.maxY) {
                                edit.setBiome(BlockVector3.at(x, y, z), biomeType)
                                y += BIOME_STEP
                            }
                            z += BIOME_STEP
                        }
                        x += BIOME_STEP
                    }
                }
            }.onFailure {
                Instance.logger.severe("[ilha] Falha ao colar o terreno inicial da ilha ${island.uuid}: ${it.message}")
            }

            Bukkit.getScheduler().runTask(Instance, Runnable { placeFloor(island) })
        })
    }

    fun placeFloor(island: Island) {
        val center = island.center.toBukkit() ?: return
        val world = center.world ?: return

        world.getBlockAt(center.blockX, center.blockY - 1, center.blockZ).type = Material.BEDROCK
        world.getBlockAt(center.blockX, center.blockY, center.blockZ).type = Material.DIRT
    }
}
