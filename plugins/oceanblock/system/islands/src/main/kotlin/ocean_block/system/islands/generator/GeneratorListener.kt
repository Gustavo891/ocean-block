package ocean_block.system.islands.generator

import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandUpgrade
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFormEvent
import kotlin.random.Random

class GeneratorListener(private val table: GeneratorTable) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onForm(event: BlockFormEvent) {
        if (event.newState.type !in FORMED) return
        val island = IslandManager.getIslandAt(event.block.location) ?: return
        val level = island.getUpgradeLevel(IslandUpgrade.Generator)
        val block = Material.matchMaterial(GeneratorRoller.roll(table, level, Random.Default)) ?: return
        event.newState.type = block
        GeneratorBlocks.mark(BlockKey.of(event.block))
        Bukkit.getPluginManager().callEvent(GeneratorFormedEvent(island))
    }

    private companion object {
        val FORMED = setOf(Material.COBBLESTONE, Material.STONE)
    }
}
