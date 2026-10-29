package ocean_block.system.islands.choice

import ocean_block.system.islands.ext.runNextTask
import ocean_block.system.islands.island.IslandManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status

class ChoiceListener : Listener {

    private val failedPack = setOf(Status.DECLINED, Status.FAILED_DOWNLOAD, Status.INVALID_URL, Status.FAILED_RELOAD, Status.DISCARDED)

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        runNextTask(JOIN_DELAY) { check(event.player) }
    }

    @EventHandler
    fun onPack(event: PlayerResourcePackStatusEvent) {
        val player = event.player
        if (!ChoiceCamera.isMarked(player) || !IslandChoice.needsChoice(player)) return
        when (event.status) {
            Status.SUCCESSFULLY_LOADED -> runNextTask { if (player.isOnline) IslandTypeDialog.open(player) }
            in failedPack -> runNextTask { if (player.isOnline) IslandTypeMenu.open(player) }
            else -> Unit
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.getHolder(false) as? ChoiceHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.clickedInventory != event.inventory) return

        val confirming = holder.confirming
        if (confirming == null) {
            val type = IslandTypeMenu.TYPE_SLOTS[event.slot] ?: return
            runNextTask { if (player.isOnline) IslandTypeMenu.confirm(player, type) }
            return
        }
        when (event.slot) {
            IslandTypeMenu.CONFIRM_SLOT -> runNextTask { if (player.isOnline) IslandChoice.confirm(player, confirming) }
            IslandTypeMenu.BACK_SLOT -> runNextTask { if (player.isOnline) IslandTypeMenu.open(player) }
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.getHolder(false) is ChoiceHolder) event.isCancelled = true
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.inventory.getHolder(false) !is ChoiceHolder) return
        if (event.reason == InventoryCloseEvent.Reason.OPEN_NEW || event.reason == InventoryCloseEvent.Reason.PLUGIN) return
        val player = event.player as? Player ?: return
        runNextTask(REOPEN_DELAY) {
            if (player.isOnline && ChoiceCamera.isMarked(player) && IslandChoice.needsChoice(player)) IslandTypeMenu.open(player)
        }
    }

    private fun check(player: Player) {
        if (!player.isOnline) return
        if (!IslandManager.loaded) {
            runNextTask(LOAD_RETRY) { check(player) }
            return
        }
        val home = IslandManager.getHomeIsland(player)
        when (ChoiceRules.afterLogin(ChoiceCamera.isMarked(player), home != null)) {
            ChoiceRules.LoginAction.CHOOSE -> IslandChoice.begin(player)
            ChoiceRules.LoginAction.RESTORE -> {
                ChoiceCamera.leave(player)
                home?.teleportTo(player)
            }
            ChoiceRules.LoginAction.NOTHING -> Unit
        }
    }

    private companion object {
        const val JOIN_DELAY = 40L
        const val LOAD_RETRY = 20L
        const val REOPEN_DELAY = 20L
    }
}
