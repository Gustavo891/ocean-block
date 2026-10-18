package one_block.system.custom_items.item.context

import one_block.system.custom_items.cooldown.CooldownStore
import one_block.system.custom_items.item.CustomItem
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class EntityContext(
    val player: Player,
    val item: ItemStack,
    val customItem: CustomItem,
    val target: Entity,
    val hand: EquipmentSlot,
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }

    fun markCooldown() = CooldownStore.stamp(player.uniqueId, customItem.id, customItem.cooldownMillis)
}
