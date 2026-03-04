package one_block.system.minions.placed_minion

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.Location

object PlacedMinionManager {

    private val minions = mutableMapOf<SimpleLocation, PlacedMinion>()
    private val repository = PlacedMinionRepository()

    fun onInit() {
        Bukkit.getScheduler().runTaskTimer(Instance, Runnable { processMinions() }, 0L, 20L)
        repository.toList().forEach { minions[it.location] = it }
    }

    fun toList() = minions.values

    fun register(placedMinion: PlacedMinion) {
        minions[placedMinion.location] = placedMinion
        repository.update(placedMinion)
    }

    fun unregister(location: SimpleLocation): PlacedMinion? {
        return minions.remove(location)
    }

    fun get(location: SimpleLocation) = minions[location]

    fun get(location: Location) = get(SimpleLocation.fromBukkit(location))

    private fun processMinions() {
        minions.values.forEach(::processMinion)
    }

    private fun processMinion(minion: PlacedMinion) {
        minion.type.executeAction(minion)
    }
}