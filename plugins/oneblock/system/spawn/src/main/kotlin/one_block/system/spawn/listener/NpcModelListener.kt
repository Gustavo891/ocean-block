package one_block.system.spawn.listener

import com.ticxo.modelengine.api.events.BaseEntityInteractEvent
import one_block.system.spawn.npc.NpcManager
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID

class NpcModelListener : Listener {

    private val lastInteractTick = mutableMapOf<UUID, Int>()

    @EventHandler
    fun onBaseEntityInteract(event: BaseEntityInteractEvent) {
        if (event.action == BaseEntityInteractEvent.Action.ATTACK) return

        val stand = event.baseEntity.original as? ArmorStand ?: return
        val npcId = NpcManager.npcIdOf(stand) ?: return

        val player = event.player
        val tick = Bukkit.getCurrentTick()
        if (lastInteractTick[player.uniqueId] == tick) return
        lastInteractTick[player.uniqueId] = tick

        NpcManager.interact(player, npcId)
    }
}
