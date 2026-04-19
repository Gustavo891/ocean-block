package one_block.system.minions.placed_minion

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.system.minions.Minions
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.EquipmentSlot


object PlacedMinionManager {

    private val minions = mutableMapOf<SimpleLocation, PlacedMinion>()
    private val minionsByUuid = mutableMapOf<UUID, PlacedMinion>()
    private val armorStands = mutableMapOf<UUID, ArmorStand>()
    private val repository = PlacedMinionRepository()
    private val displays = mutableMapOf<UUID, TextDisplay>()

    fun onInit() {
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            val savedMinions = repository.toList()
            cleanMinions()
            savedMinions.forEach { minion ->
                spawn(minion)
                update(minion)
            }
            Bukkit.getScheduler().runTaskTimer(Instance, Runnable { processTick() }, 0L, 20L)
        }, 40L)
    }

    fun onDisable() {
        armorStands.values.forEach { it.remove() }
        armorStands.clear()
        displays.values.forEach { it.remove() }
    }

    fun getDisplay(UUID: UUID): TextDisplay? = displays[UUID]

    fun get(location: SimpleLocation) = minions[location]

    fun get(location: Location) = minions.values.firstOrNull { it.location.isSameBlock(location) }

    fun get(uuid: UUID) = minionsByUuid[uuid]

    fun getArmorStand(uuid: UUID): ArmorStand? = armorStands[uuid]

    fun spawn(placedMinion: PlacedMinion) {
        val location = placedMinion.location.toBukkit() ?: return
        placedMinion.type.spawn(placedMinion, location)
    }

    fun register(placedMinion: PlacedMinion, armorStand: ArmorStand, textDisplay: TextDisplay) {
        Minions.markAsMinion(armorStand, placedMinion.uuid)
        minions[placedMinion.location] = placedMinion
        minionsByUuid[placedMinion.uuid] = placedMinion
        armorStands[placedMinion.uuid] = armorStand
        displays[placedMinion.uuid] = textDisplay
        updateArmorStandHand(placedMinion)
        repository.update(placedMinion)
    }

    fun update(placedMinion: PlacedMinion) {
        minions[placedMinion.location] = placedMinion
        minionsByUuid[placedMinion.uuid] = placedMinion
        placedMinion.type.updateDisplay(placedMinion)
        repository.update(placedMinion)
    }

    fun unregister(location: SimpleLocation): PlacedMinion? {
        val removed = minions.remove(location) ?: return null
        minionsByUuid.remove(removed.uuid)
        displays[removed.uuid]?.remove()
        displays.remove(removed.uuid)
        armorStands.remove(removed.uuid)
        removed.type.onUnregister(removed.uuid)
        repository.remove(removed.uuid)
        return removed
    }

    fun updateArmorStandHand(placedMinion: PlacedMinion) {
        val armorStand = armorStands[placedMinion.uuid] ?: return
        val handItem = placedMinion.getTool() ?: itemStack(Material.AIR) {}
        armorStand.setItem(EquipmentSlot.HAND, handItem)
    }

    private fun processTick() {
        minions.values.toList()
            .filter { it.enabled && it.hasEnergy() }
            .forEach { minion ->
                if (!minion.type.tick(minion)) return@forEach
                if (minion.applyToolDurability()) updateArmorStandHand(minion)
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
