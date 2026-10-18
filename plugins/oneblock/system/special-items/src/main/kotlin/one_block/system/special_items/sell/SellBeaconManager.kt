package one_block.system.special_items.sell

import net.kyori.adventure.text.Component
import one_block.libs.formatter.NumberFormatter
import one_block.libs.instance.Instance
import one_block.libs.instance.ScheduledExecutor
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.system.special_items.SpecialItemSystem
import one_block.system.special_items.listener.BeaconListener
import one_block.system.special_items.tasks.AnimationTask
import one_block.system.special_items.tasks.BeamTask
import one_block.system.special_items.tasks.SellTask
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
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

    val beaconbyLoc = ConcurrentHashMap<String, SellBeacon>()
    val beaconbyUUID = ConcurrentHashMap<UUID, SellBeacon>()

    val keyMarker = NamespacedKey(SpecialItemSystem.plugin, "sell_beacon_marker")
    val keyOwner = NamespacedKey(SpecialItemSystem.plugin, "sell_beacon_owner")
    val coinsAmount = NamespacedKey(SpecialItemSystem.plugin, "sell_beacon_coins")

    private val dirtyBeacons = mutableSetOf<UUID>()

    val repository = BeaconRepository()
    private lateinit var sellTask: SellTask

    fun onInit() {
        load(repository.toList())
        AnimationTask()
        sellTask = SellTask()
        BeamTask(sellTask)

        ScheduledExecutor.scheduleAtFixedRate({ saveDirtyBeacons() }, 30, 30, TimeUnit.SECONDS)
    }

    fun load(beacons: List<SellBeacon>) {
        beacons.forEach { add(it, persist = false) }
    }

    fun markDirty(beaconUUID: UUID) {
        dirtyBeacons.add(beaconUUID)
    }

    fun onDisable() {
        saveDirtyBeacons()
    }

    private fun saveDirtyBeacons() {
        if (dirtyBeacons.isEmpty()) return

        val toSave = dirtyBeacons.toSet()
        val beacons = toSave.mapNotNull { beaconbyUUID[it] }

        val saved = runCatching { repository.saveAll(beacons) }
        if (saved.isFailure) {
            Instance.logger.severe(
                "[Beacons] Falha ao salvar ${beacons.size} beacon(s): ${saved.exceptionOrNull()?.message}. " +
                    "Eles seguem marcados para a proxima tentativa."
            )
            return
        }

        dirtyBeacons.removeAll(toSave)
        Instance.logger.info("[Beacons] Salvos ${beacons.size} beacons que sofreram alterações.")
    }

    fun add(beacon: SellBeacon, persist: Boolean = true) {
        beaconbyLoc[beacon.key()] = beacon
        beaconbyUUID[beacon.uuid] = beacon
        spawnBeaconDisplay(beacon.coordinate.toBukkit(), beacon)

        if (persist) {
            repository.save(beacon)
            Instance.logger.info("[Beacons] Beacon ${beacon.uuid} salvo ao ser colocado.")
        }
    }


    fun remove(beacon: SellBeacon) {
        beaconbyUUID.remove(beacon.uuid)
        beaconbyLoc.remove(beacon.key())
        dirtyBeacons.remove(beacon.uuid)
        repository.remove(beacon.uuid)
    }

    fun createBeaconItem(sellBeacon: SellBeacon?) = itemStack(Material.BEACON) {
        meta<ItemMeta> {
            displayName(Component.text("§aBeacon vendedor"))
            persistentDataContainer.set(BeaconListener.BEACON_KEY, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(coinsAmount, PersistentDataType.DOUBLE, sellBeacon?.coinsAmount ?: 0.0)
        }
    }

    fun spawnBeaconDisplay(baseBlockLoc: Location?, sellBeacon: SellBeacon): Boolean {
        if (baseBlockLoc == null) return false
        val world = baseBlockLoc.world ?: return false
        val spawnLoc = baseBlockLoc.clone().add(0.5, 2.0, 0.5)

        sellBeacon.originalY = spawnLoc.y

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
        }

        sellBeacon.display = display
        sellBeacon.interaction = interaction

        return true
    }

    fun handleRightClick(player: Player, interaction: Interaction) {
        if (!isSellBeacon(interaction)) return

        val uuid = getInteractionUUID(interaction) ?: return
        val sellBeacon = beaconbyUUID[uuid] ?: return
        if (sellBeacon.ownerUUID != player.uniqueId) {
            player.sendMessage("§cVocê não é o dono desse beacon.")
            return
        }

        player.sendMessage("§aO seu beacon de venda possui: §2$§f${NumberFormatter.format(sellBeacon.coinsAmount)}")
    }

    fun handleLeftHit(player: Player, interaction: Interaction) {
        if (!isSellBeacon(interaction)) return

        val uuid = getInteractionUUID(interaction) ?: return
        val sellBeacon = beaconbyUUID[uuid] ?: return
        if (sellBeacon.ownerUUID != player.uniqueId) {
            player.sendMessage("§cVocê não é o dono desse beacon.")
            return
        }

        sellBeacon.display?.remove()
        interaction.remove()
        remove(sellBeacon)
        player.inventory.addItem(createBeaconItem(sellBeacon))
        player.sendMessage("§eBeacon removido e devolvido.")
    }

    fun isSellBeacon(interaction: Interaction): Boolean =
        interaction.persistentDataContainer.has(keyMarker, PersistentDataType.BYTE)

    private fun getInteractionUUID(interaction: Interaction): UUID? =
        interaction.persistentDataContainer
            .get(keyOwner, PersistentDataType.STRING)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
}
