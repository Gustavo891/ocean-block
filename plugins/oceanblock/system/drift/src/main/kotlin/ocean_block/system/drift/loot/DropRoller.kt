package ocean_block.system.drift.loot

import ocean_block.system.drift.config.DebrisType
import org.bukkit.Material
import kotlin.random.Random

object DropRoller {

    fun roll(type: DebrisType, random: Random): List<Pair<Material, Int>> {
        val drops = type.drops
            .filter { random.nextDouble() < it.chance }
            .map { it.material to random.nextInt(it.min, it.max + 1) }

        return drops.ifEmpty { listOf(Material.STRING to 1) }
    }
}
