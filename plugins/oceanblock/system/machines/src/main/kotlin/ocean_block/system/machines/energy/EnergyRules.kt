package ocean_block.system.machines.energy

import java.util.UUID

data class EnergySource(val id: UUID, val distance: Double, val stored: Long)

object EnergyRules {

    const val RADIUS = 8.0
    const val KEY = "energia"

    fun plan(needed: Long, sources: List<EnergySource>): Map<UUID, Long>? {
        if (needed <= 0) return emptyMap()
        val usable = sources
            .filter { it.distance <= RADIUS && it.stored > 0 }
            .sortedWith(compareBy<EnergySource> { it.distance }.thenBy { it.id })
        if (usable.sumOf { it.stored } < needed) return null
        var remaining = needed
        val takes = linkedMapOf<UUID, Long>()
        for (source in usable) {
            if (remaining == 0L) break
            val take = minOf(remaining, source.stored)
            takes[source.id] = take
            remaining -= take
        }
        return takes
    }
}
