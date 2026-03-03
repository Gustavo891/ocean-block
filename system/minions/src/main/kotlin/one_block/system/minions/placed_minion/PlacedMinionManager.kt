package one_block.system.minions.placed_minion

import one_block.libs.coordinate.SimpleLocation

object PlacedMinionManager {

    private val minions = mutableMapOf<SimpleLocation, PlacedMinion>()

    fun toList() = minions.values

    fun register(placedMinion: PlacedMinion) {
        minions[placedMinion.location] = placedMinion
    }

    fun unregister(location: SimpleLocation): PlacedMinion? {
        return minions.remove(location)
    }
}