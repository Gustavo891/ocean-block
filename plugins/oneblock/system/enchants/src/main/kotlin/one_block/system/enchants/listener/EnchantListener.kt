package one_block.system.enchants.listener

import one_block.system.enchants.Enchants
import one_block.system.enchants.enchant.context.AttackContext
import one_block.system.enchants.enchant.context.BreakContext
import one_block.system.enchants.enchant.context.DamagedContext
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent

class EnchantListener : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type.isAir) return

        val enchants = Enchants.all(item)
        if (enchants.isEmpty()) return

        for ((enchant, level) in enchants) {
            enchant.onBlockBreak(BreakContext(player, event.block, item, level, event))
            if (!event.isDropItems) break
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        handleAttacker(event)
        handleVictim(event)
    }

    private fun handleAttacker(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? LivingEntity ?: return

        val item = attacker.inventory.itemInMainHand
        if (item.type.isAir) return

        Enchants.all(item).forEach { (enchant, level) ->
            enchant.onAttack(AttackContext(attacker, victim, item, level, event))
        }
    }

    private fun handleVictim(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return

        victim.inventory.armorContents.filterNotNull().forEach { piece ->
            if (piece.type.isAir) return@forEach

            Enchants.all(piece).forEach { (enchant, level) ->
                enchant.onDamaged(DamagedContext(victim, event.damager, piece, level, event))
            }
        }
    }
}
