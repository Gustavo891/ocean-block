package one_block.system.repair.listener

import one_block.system.repair.Repairs
import one_block.system.repair.enums.Repair
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.Damageable

class PlayerListener: Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val whoClicked = event.whoClicked as? Player ?: return

        val cursorItem = event.cursor
        val rawRepair = Repairs.getRepairByItem(cursorItem) ?: return
        val repair = EnumUtils.getEnumIgnoreCase(Repair::class.java, rawRepair) ?: return

        val currentItem = event.currentItem ?: return
        event.isCancelled = true

        val itemMeta = currentItem.itemMeta
        if (itemMeta !is Damageable) return

        val max = currentItem.type.maxDurability.toInt()
        if (max == 0) return

        val currentDamage = itemMeta.damage
        val repairAmount = (max * repair.amount).toInt()

        itemMeta.damage = (currentDamage - repairAmount).coerceAtLeast(0)
        currentItem.itemMeta = itemMeta

        cursorItem.amount--
        whoClicked.playSound(whoClicked, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f)
        whoClicked.updateInventory()
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return

        if (Repairs.getRepairByItem(item) != null) {
            event.isCancelled = true
            return
        }

        if (Repairs.getFragmentByItem(item) != null) {
            event.isCancelled = true
            return
        }
    }
}
