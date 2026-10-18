package ocean_block.system.drift.loot

import ocean_block.system.drift.config.DebrisType
import kotlin.random.Random

object DebrisPicker {

    fun pick(types: List<DebrisType>, random: Random): DebrisType? {
        val total = types.sumOf { it.weight.coerceAtLeast(0) }
        if (total <= 0) return null

        val roll = random.nextInt(total)
        var cumulative = 0
        for (type in types) {
            cumulative += type.weight.coerceAtLeast(0)
            if (roll < cumulative) return type
        }
        return null
    }
}
