package ocean_block.system.battle_pass.listener

import ocean_block.system.battle_pass.challenge.ChallengeManager
import ocean_block.system.battle_pass.challenge.ProgressManager
import ocean_block.system.battle_pass.config.Challenges
import ocean_block.system.battle_pass.config.ChallengeObjective
import ocean_block.system.craft.events.CustomCraftEvent
import ocean_block.system.sell.listener.SellEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.CraftItemEvent

class ChallengeListener : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val challenge = activeChallengeOf(player) ?: return

        when (val obj = challenge.objective) {
            is ChallengeObjective.BreakOneBlock -> return

            is ChallengeObjective.BreakBlock -> {
                if (obj.material == event.block.type.name) {
                    ProgressManager.increment(player, challenge)
                }
            }

            else -> return
        }

    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        val challenge = activeChallengeOf(killer) ?: return
        val obj = challenge.objective as? ChallengeObjective.KillMob ?: return

        if (obj.mobType == event.entity.type.name) {
            ProgressManager.increment(killer, challenge)
        }
    }

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return
        val challenge = activeChallengeOf(player) ?: return
        val obj = challenge.objective as? ChallengeObjective.CraftItem ?: return

        val result = event.recipe.result
        if (obj.material == result.type.name) {
            ProgressManager.increment(player, challenge, result.amount)
        }
    }

    @EventHandler
    fun onSell(event: SellEvent) {
        val player = event.player
        val coins = event.amount.toInt()
        val item = event.item
        if (coins <= 0) return

        val challenge = activeChallengeOf(player) ?: return

        when (val obj = challenge.objective) {
            is ChallengeObjective.SellValue -> {
                ProgressManager.increment(player, challenge, coins)
                return
            }

            is ChallengeObjective.SellItem -> {
                if (item.type == obj.material) {
                    ProgressManager.increment(player, challenge, item.amount)
                }
            }

            else -> return
        }

    }

    @EventHandler
    fun onCustomCraft(event: CustomCraftEvent) {
        val player = event.player
        val id = event.id
        val challenge = activeChallengeOf(player) ?: return
        val obj = challenge.objective as? ChallengeObjective.CustomCraft ?: return

        if (obj.id == id) {
            ProgressManager.increment(player, challenge, event.amount)
        }
    }

}

private fun activeChallengeOf(player: Player): Challenges? {
    val active = ChallengeManager.getActiveChallenge(player.uniqueId) ?: return null
    val progress = ProgressManager.getProgress(player.uniqueId, active.id)
    if (progress?.completed == true) return null
    return active
}

