package one_block.system.items.sell

import net.kyori.adventure.text.Component
import one_block.libs.instance.ScheduledExecutor
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.system.items.SpecialItemSystem
import one_block.system.items.listener.BeaconListener
import one_block.system.items.tasks.AnimationTask
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.collections.set

object SellBeaconManager {

    val hopperbyLoc = ConcurrentHashMap<String, SellBeacon>()
    val hopperbyUUID = ConcurrentHashMap<UUID, SellBeacon>()

    private val keyMarker = NamespacedKey(SpecialItemSystem.plugin, "sell_beacon_marker")
    private val keyOwner = NamespacedKey(SpecialItemSystem.plugin, "sell_beacon_owner")
    private val keyDisplay = NamespacedKey(SpecialItemSystem.plugin, "sell_beacon_display_uuid")

    private val repository = BeaconRepository()

    fun onInit() {
        load(repository.toList())
        AnimationTask()
    }
    fun load(hoppers: List<SellBeacon>) {
        hoppers.forEach {
            add(it)
        }
    }

    fun add(hopper: SellBeacon) {
        hopperbyLoc[hopper.key()] = hopper
        hopperbyUUID[hopper.uuid] = hopper
        spawnBeaconDisplay(hopper.coordinate.toBukkit(), hopper)
    }

    fun remove(hopper: SellBeacon) {
        hopperbyUUID.remove(hopper.uuid)
        hopperbyLoc.remove(hopper.key())
    }

    fun getByLocation(worldName: String, x: Int, y: Int, z: Int): SellBeacon? {
        return hopperbyLoc["$worldName:$x:$y:$z"]
    }

    fun createBeaconItem() = itemStack(Material.BEACON) {
        meta<ItemMeta> {
            displayName(Component.text("§aBeacon vendedor"))
            persistentDataContainer.set(BeaconListener.BEACON_KEY, PersistentDataType.STRING, "'-'")
        }
    }

    fun spawnBeaconDisplay(baseBlockLoc: Location?, sellBeacon: SellBeacon): Boolean {

        if(baseBlockLoc == null) return false
        val world = baseBlockLoc.world ?: return false
        val spawnLoc = baseBlockLoc.clone().add(0.5, 2.0, 0.5)

        val display = world.spawn(spawnLoc, ItemDisplay::class.java).apply {

            setItemStack(ItemStack(Material.BEACON))
            val t = transformation
            t.scale.set(Vector3f(0.5f, 0.5f, 0.5f))
            transformation = t

            setGravity(false)
            isPersistent = true
        }

        val interaction = world.spawn(spawnLoc, Interaction::class.java).apply {
            interactionWidth = 0.8f
            interactionHeight = 0.8f
            isResponsive = true

            persistentDataContainer.set(keyMarker, PersistentDataType.BYTE, 1)
            persistentDataContainer.set(keyOwner, PersistentDataType.STRING, sellBeacon.uuid.toString())
            persistentDataContainer.set(keyDisplay, PersistentDataType.STRING, display.uniqueId.toString())
        }

        sellBeacon.display = display
        sellBeacon.interaction = interaction

        return true
    }

    fun handleRightClick(player: Player, interaction: Interaction) {
        if (!isSellBeacon(interaction)) return

        val uuid = getUUID(interaction)
        val sellBeacon = hopperbyUUID[uuid] ?: return
        if (sellBeacon.ownerUUID != player.uniqueId) {
            player.sendMessage("§cVocê não é o dono desse beacon.")
            return
        }

        player.sendMessage("§aVocê clicou no beacon de venda!")
    }

    fun handleLeftHit(player: Player, interaction: Interaction) {
        if (!isSellBeacon(interaction)) return

        val uuid = getUUID(interaction)
        val sellBeacon = hopperbyUUID[uuid] ?: return
        if (sellBeacon.ownerUUID != player.uniqueId) {
            player.sendMessage("§cVocê não é o dono desse beacon.")
            return
        }

        val displayUuid = interaction.persistentDataContainer
            .get(keyDisplay, PersistentDataType.STRING)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

        if (displayUuid != null) {
            val entity = Bukkit.getEntity(displayUuid)
            if (entity is ItemDisplay) entity.remove()
        }

        interaction.remove()

        player.inventory.addItem(createBeaconItem())
        player.sendMessage("§eBeacon removido e devolvido.")
    }

    fun isSellBeacon(interaction: Interaction): Boolean =
        interaction.persistentDataContainer.has(keyMarker, PersistentDataType.BYTE)

    private fun getUUID(interaction: Interaction): UUID? =
        interaction.persistentDataContainer
            .get(keyOwner, PersistentDataType.STRING)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }



}