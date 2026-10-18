package one_block.textura.listener

import one_block.textura.api.BlockApi
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.player.PlayerInteractEvent

class NoteBlockGuard : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onNotePlay(event: NotePlayEvent) {
        if (BlockApi.idAt(event.block) == null) return
        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPhysics(event: BlockPhysicsEvent) {
        if (event.block.type != Material.NOTE_BLOCK) return
        if (BlockApi.idAt(event.block) == null) return
        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.NOTE_BLOCK) return
        if (BlockApi.idAt(block) == null) return

        // Agachado com um bloco na mao o vanilla ja pula a interacao do note block
        // e vai direto colocar: nao ha nota a proteger, e negar o evento aqui so
        // impediria a colocacao (DENY no bloco marca o evento como cancelado).
        val item = event.item
        if (event.player.isSneaking && item != null && item.type.isBlock) return

        event.setUseInteractedBlock(Event.Result.DENY)
    }

    @EventHandler
    fun onRedstone(event: BlockRedstoneEvent) {
        if (event.block.type != Material.NOTE_BLOCK) return
        if (BlockApi.idAt(event.block) == null) return
        event.newCurrent = event.oldCurrent
    }
}
