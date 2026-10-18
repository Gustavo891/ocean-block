package ocean_block.system.custom_items.item

import ocean_block.libs.instance.Instance
import ocean_block.system.custom_items.cooldown.CooldownStore
import ocean_block.system.custom_items.item.context.ConsumeContext
import ocean_block.system.custom_items.item.context.EntityContext
import ocean_block.system.custom_items.item.context.InteractContext
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

class CustomItemListener : Listener {

    private val lastInteractTick = ConcurrentHashMap<UUID, Int>()

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        val custom = CustomItemRegistry.fromItemStack(item) ?: return
        val player = event.player

        val isRight = event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK
        val isLeft = event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK
        if (!isRight && !isLeft) return

        val tick = Bukkit.getCurrentTick()
        if (lastInteractTick.put(player.uniqueId, tick) == tick) return

        if (blockedByCooldown(player, custom)) {
            event.isCancelled = true
            return
        }

        val ctx = InteractContext(player, item, custom, event.clickedBlock, event.hand ?: return, event)
        runHook(custom.id) {
            if (isRight) custom.onRightClick(ctx) else custom.onLeftClick(ctx)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val item = when (event.hand) {
            EquipmentSlot.OFF_HAND -> player.inventory.itemInOffHand
            else -> player.inventory.itemInMainHand
        }
        val custom = CustomItemRegistry.fromItemStack(item) ?: return

        if (blockedByCooldown(player, custom)) {
            event.isCancelled = true
            return
        }

        val ctx = EntityContext(player, item, custom, event.rightClicked, event.hand, event)
        runHook(custom.id) { custom.onInteractEntity(ctx) }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onConsume(event: PlayerItemConsumeEvent) {
        val item = event.item
        val custom = CustomItemRegistry.fromItemStack(item) ?: return
        val player = event.player

        if (blockedByCooldown(player, custom)) {
            event.isCancelled = true
            return
        }

        val ctx = ConsumeContext(player, item, custom, event)
        runHook(custom.id) { custom.onConsume(ctx) }
    }

    private fun blockedByCooldown(player: Player, custom: CustomItem): Boolean {
        if (custom.cooldownMillis <= 0L) return false
        if (CooldownStore.isReady(player.uniqueId, custom.id)) return false

        val secs = ceil(CooldownStore.remaining(player.uniqueId, custom.id) / 1000.0).toInt()
        player.sendRichMessage("<red>Aguarde <white>${secs}s<red> para usar novamente.")
        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        return true
    }

    private inline fun runHook(id: String, block: () -> Unit) {
        runCatching { block() }
            .onFailure { Instance.logger.warning("[custom-items] hook de '$id' falhou: ${it.message}") }
    }
}
