package one_block.system.items.listener

import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.island.IslandManager
import one_block.system.items.sell.SellBeacon
import one_block.system.items.sell.SellBeaconManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class BeaconListener(plugin: JavaPlugin) : Listener {

    companion object {
        val BEACON_KEY = NamespacedKey("oneblock", "sellbeacon")
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onUseBeaconOnBlock(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item = event.item ?: return
        if (item.type != Material.BEACON) return

        val clicked = event.clickedBlock ?: return
        event.isCancelled = true
        val island = IslandManager.getIslandAt(clicked.location)
        if(island == null) {
            player.sendRichMessage("<red>Necessário estar em uma ilha.")
            return
        }

        player.inventory.remove(item)

        val sellBeacon = SellBeacon(
            UUID.randomUUID(),
            SimpleLocation(clicked.location.x, clicked.location.y, clicked.location.z, 0F, 0F, clicked.location.world.name),
            player.uniqueId,
            IslandManager.getIslandAt(clicked.location)!!.uuid
        )

        sellBeacon.originalY = clicked.location.y + 2
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



}