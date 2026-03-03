package one_block.system.items.listener

import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.island.IslandManager
import one_block.system.items.sell.BeaconRepository
import one_block.system.items.sell.SellBeacon
import one_block.system.items.sell.SellBeaconManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class BeaconListener : Listener {

    companion object {
        val BEACON_KEY = NamespacedKey("oneblock", "sellbeacon")
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onUseBeaconOnBlock(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item = event.item ?: return
        if (item.type != Material.BEACON) return
        if (!item.persistentDataContainer.has(BEACON_KEY, PersistentDataType.BOOLEAN)) return

        val clicked = event.clickedBlock ?: return
        event.isCancelled = true

        val island = IslandManager.getIslandAt(clicked.location)
        if (island == null) {
            player.sendRichMessage("<red>Necessário estar em uma ilha.")
            return
        }

        player.inventory.remove(item)

        val coinsAmount = item.persistentDataContainer
            .get(SellBeaconManager.coinsAmount, PersistentDataType.DOUBLE) ?: 0.0

        val sellBeacon = SellBeacon(
            uuid = UUID.randomUUID(),
            coordinate = SimpleLocation(
                clicked.location.x,
                clicked.location.y,
                clicked.location.z,
                0F,
                0F,
                clicked.location.world.name
            ),
            ownerUUID = player.uniqueId,
            islandUUID = island.uuid,
            coinsAmount = coinsAmount
        )

        SellBeaconManager.add(sellBeacon)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onRightClickEntity(event: PlayerInteractEntityEvent) {
        val interaction = event.rightClicked as? Interaction ?: return
        if (!SellBeaconManager.isSellBeacon(interaction)) return

        event.isCancelled = true
        SellBeaconManager.handleRightClick(event.player, interaction)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onHitEntity(event: EntityDamageByEntityEvent) {
        val interaction = event.entity as? Interaction ?: return
        if (!SellBeaconManager.isSellBeacon(interaction)) return

        val player = when (val d = event.damager) {
            is Player -> d
            is Projectile -> d.shooter as? Player
            else -> null
        } ?: return

        event.isCancelled = true
        SellBeaconManager.handleLeftHit(player, interaction)
    }

    @EventHandler
    fun onDisable(event: PluginDisableEvent) {
        SellBeaconManager.beaconbyUUID.values.forEach { beacon ->
            beacon.display?.remove()
            beacon.interaction?.remove()
            SellBeaconManager.repository.save(beacon)
        }
    }
}