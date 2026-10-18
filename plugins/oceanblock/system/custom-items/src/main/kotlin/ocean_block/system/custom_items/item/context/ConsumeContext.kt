package ocean_block.system.custom_items.item.context

import ocean_block.system.custom_items.cooldown.CooldownStore
import ocean_block.system.custom_items.item.CustomItem
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.inventory.ItemStack

class ConsumeContext(
    val player: Player,
    val item: ItemStack,
    val customItem: CustomItem,
    private val event: Cancellable,
) {
    fun cancel() { event.isCancelled = true }

    fun markCooldown() = CooldownStore.stamp(player.uniqueId, customItem.id, customItem.cooldownMillis)
}
