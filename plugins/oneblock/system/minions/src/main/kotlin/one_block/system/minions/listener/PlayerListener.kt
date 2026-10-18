package one_block.system.minions.listener

import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.island.IslandManager
import one_block.system.minions.Minions
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID as JavaUUID

class PlayerListener(private val viewFrame: ViewFrame) : Listener {

    @EventHandler
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val rightClicked = event.rightClicked
        if (rightClicked !is ArmorStand) return

        val minionId = Minions.getMinionId(rightClicked) ?: return
        val placedMinion = PlacedMinionManager.get(minionId) ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val entity = event.entity

        if (entity !is ArmorStand) return

        val minionId = Minions.getMinionId(entity)
        if (minionId == null) {
            val damager = event.damager
            if (damager is Player) {
                damager.sendMessage("Este ArmorStand não é um minion.")
            }
            return
        }

        val placedMinion = PlacedMinionManager.get(minionId)

        val damager = event.damager
        if (damager !is Player) return

        if (placedMinion == null) return

        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        val rightClicked = event.rightClicked

        if (rightClicked !is ArmorStand) return

        val minionId = Minions.getMinionId(rightClicked) ?: return
        val placedMinion = PlacedMinionManager.get(minionId) ?: return

        event.isCancelled = true

        MinionInteraction.handle(event.player, placedMinion, viewFrame)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val item = event.player.inventory.itemInMainHand
        val minionType = Minions.getMinionType(item) ?: return

        event.isCancelled = true

        val placement = minionType.placement(clickedBlock, event.blockFace)
        if (placement == null) {
            event.player.sendMessage("§cSem espaço para o minion nascer aí.")
            return
        }

        val spawnLocation = placement.location
        val simpleLocation = SimpleLocation.fromBukkit(spawnLocation)

        val island = IslandManager.getIslandAt(spawnLocation)
        if (island == null || (island.ownerUuid != event.player.uniqueId && !island.isMember(event.player.name))) {
            event.player.sendMessage("§cVocê não pode colocar minions aqui.")
            return
        }

        if (PlacedMinionManager.get(spawnLocation) != null) {
            event.player.sendMessage("§cJá existe um minion aqui.")
            return
        }

        val fromItem = Minions.readPlacedMinionFromItem(item, simpleLocation)

        val placedMinion = fromItem ?: PlacedMinion(
            JavaUUID.randomUUID(),
            simpleLocation,
            minionType,
            0
        )
        placedMinion.facing = placement.facing?.name
        if (placedMinion.owner == null) placedMinion.owner = event.player.uniqueId.toString()
        if (placedMinion.placedAt == null) placedMinion.placedAt = System.currentTimeMillis()

        PlacedMinionManager.spawn(placedMinion)


        item.amount -= 1
        event.player.sendMessage("§eMinion §f${minionType.prettyName} §ecolocado com sucesso!")
    }
}
