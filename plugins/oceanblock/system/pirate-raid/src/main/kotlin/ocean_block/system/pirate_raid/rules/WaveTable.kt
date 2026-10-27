package ocean_block.system.pirate_raid.rules

import kotlin.math.roundToInt
import kotlin.random.Random

data class Band(val minLevel: Int, val waves: List<Int>, val bruteShare: Double)

data class Crew(val pillagers: Int, val brutes: Int, val superPirate: Boolean) {
    val total: Int get() = pillagers + brutes + if (superPirate) 1 else 0
}

object WaveTable {

    private const val SEATS = 2

    fun band(bands: List<Band>, level: Int): Band =
        bands.lastOrNull { it.minLevel <= level } ?: bands.first()

    fun crew(band: Band, wave: Int, random: Random): Crew {
        val size = band.waves[(wave - 1).coerceIn(band.waves.indices)]
        val brutes = (size * band.bruteShare).roundToInt().coerceIn(0, size)
        return Crew(size - brutes, brutes, wave >= band.waves.size)
    }

    fun boats(crew: Crew): Int {
        val regular = crew.pillagers + crew.brutes
        return (regular + SEATS - 1) / SEATS + if (crew.superPirate) 1 else 0
    }
}
