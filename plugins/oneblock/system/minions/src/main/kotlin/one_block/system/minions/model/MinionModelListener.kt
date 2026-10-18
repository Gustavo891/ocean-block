package one_block.system.minions.model

import com.ticxo.modelengine.api.events.BaseEntityInteractEvent
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.minions.Minions
import one_block.system.minions.listener.MinionInteraction
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID

class MinionModelListener(private val viewFrame: ViewFrame) : Listener {

    private val lastInteractTick = mutableMapOf<UUID, Int>()

    @EventHandler
    fun onBaseEntityInteract(event: BaseEntityInteractEvent) {
        if (event.action == BaseEntityInteractEvent.Action.ATTACK) return

        val stand = event.baseEntity.original as? ArmorStand ?: return
        val minionId = Minions.getMinionId(stand) ?: return
        val placedMinion = PlacedMinionManager.get(minionId) ?: return

        val player = event.player
        val tick = Bukkit.getCurrentTick()
        if (lastInteractTick[player.uniqueId] == tick) return
        lastInteractTick[player.uniqueId] = tick

        MinionInteraction.handle(player, placedMinion, viewFrame)
    }
}
