package one_block.system.custom_items.item.context

import one_block.system.custom_items.cooldown.CooldownStore
import one_block.system.custom_items.item.CustomItem
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class InteractContext(
    val player: Player,
    val item: ItemStack,
    val customItem: CustomItem,
    val clickedBlock: Block?,
    val hand: EquipmentSlot,
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }

    fun markCooldown() = CooldownStore.stamp(player.uniqueId, customItem.id, customItem.cooldownMillis)

    fun setItem(stack: ItemStack) {
        when (hand) {
            EquipmentSlot.OFF_HAND -> player.inventory.setItemInOffHand(stack)
            else -> player.inventory.setItemInMainHand(stack)
        }
    }
}
