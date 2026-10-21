package ocean_block.system.stations.sieve

import kotlin.random.Random

object SieveRoller {

    fun roll(drops: List<SieveDrop>, random: Random): List<Pair<String, Int>> =
        drops.filter { random.nextDouble() < it.chance }.map { it.output to random.nextInt(it.min, it.max + 1) }
}
