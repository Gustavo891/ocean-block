package one_block.system.minions.placed_minion

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.system.minions.Minions
import one_block.system.minions.model.MinionModel
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay


object PlacedMinionManager {

    private val minions = mutableMapOf<SimpleLocation, PlacedMinion>()
    private val minionsByUuid = mutableMapOf<UUID, PlacedMinion>()
    private val armorStandUuids = mutableMapOf<UUID, UUID>()
    private val displayUuids = mutableMapOf<UUID, UUID>()
    private val repository = PlacedMinionRepository()

    fun onInit() {
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            val savedMinions = repository.toList()
            savedMinions.forEach { it.location.toBukkit()?.chunk }
            cleanMinions()
            savedMinions.forEach { minion ->
                spawn(minion)
                update(minion)
            }
            Bukkit.getScheduler().runTaskTimer(Instance, Runnable { processTick() }, 0L, 20L)
        }, 40L)
    }

    fun onDisable() {
        if (MinionModel.isAvailable()) MinionModel.detachAll()
        armorStandUuids.values.mapNotNull { Bukkit.getEntity(it) }.forEach { it.remove() }
        displayUuids.values.mapNotNull { Bukkit.getEntity(it) }.forEach { it.remove() }
        armorStandUuids.clear()
        displayUuids.clear()
    }

    fun getDisplay(uuid: UUID): TextDisplay? =
        displayUuids[uuid]?.let { Bukkit.getEntity(it) as? TextDisplay }

    fun get(location: SimpleLocation) = minions[location]

    fun get(location: Location) = minions.values.firstOrNull { it.location.isSameBlock(location) }

    fun get(uuid: UUID) = minionsByUuid[uuid]

    fun all(): List<PlacedMinion> = minions.values.toList()

    fun getArmorStand(uuid: UUID): ArmorStand? =
        armorStandUuids[uuid]?.let { Bukkit.getEntity(it) as? ArmorStand }

    fun spawn(placedMinion: PlacedMinion) {
        val location = placedMinion.location.toBukkit() ?: return
        placedMinion.type.spawn(placedMinion, location)
    }

    fun register(placedMinion: PlacedMinion, armorStand: ArmorStand, textDisplay: TextDisplay) {
        Minions.markAsMinion(armorStand, placedMinion.uuid)
        minions[placedMinion.location] = placedMinion
        minionsByUuid[placedMinion.uuid] = placedMinion
        armorStandUuids[placedMinion.uuid] = armorStand.uniqueId
        displayUuids[placedMinion.uuid] = textDisplay.uniqueId
        placedMinion.sync()
        repository.update(placedMinion)
    }

    fun update(placedMinion: PlacedMinion) {
        minions[placedMinion.location] = placedMinion
        minionsByUuid[placedMinion.uuid] = placedMinion
        placedMinion.type.updateDisplay(placedMinion)
        placedMinion.sync()
        repository.update(placedMinion)
    }

    fun unregister(location: SimpleLocation): PlacedMinion? {
        val removed = minions.remove(location) ?: return null
        minionsByUuid.remove(removed.uuid)
        getDisplay(removed.uuid)?.remove()
        getArmorStand(removed.uuid)?.remove()
        displayUuids.remove(removed.uuid)
        armorStandUuids.remove(removed.uuid)
        removed.type.onUnregister(removed.uuid)
        repository.remove(removed.uuid)
        return removed
    }

    private fun processTick() {
        minions.values.toList()
            .filter {
                it.enabled && it.hasEnergy() && !it.isPaused()
            }
            .forEach { minion ->
                if (!minion.type.tick(minion)) return@forEach
                minion.consumeEnergy(1)
                update(minion)
            }
    }

    private fun cleanMinions() {
        Bukkit.getWorlds().flatMap { it.entities }
            .filterIsInstance<ArmorStand>()
            .filter { Minions.getMinionId(it) != null }
            .forEach { it.remove() }
        Bukkit.getWorlds().flatMap { it.entities }
            .filterIsInstance<TextDisplay>()
            .filter { it.persistentDataContainer.has(NamespacedKey("oneblock","minion")) }
            .forEach { it.remove() }
    }
}
