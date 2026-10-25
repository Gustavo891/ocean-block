package ocean_block.system.drift.net

import kotlin.random.Random

object NetShowcase {

    const val SLOTS = 4

    fun pick(materials: List<String>, seed: Long): List<String> =
        materials.distinct().sorted().shuffled(Random(seed)).take(SLOTS)
}
