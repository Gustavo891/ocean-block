package one_block.system.minions.listener

import one_block.system.minions.enums.FullMode
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.entity.Enemy
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class TerminatorKillListener : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDeath(event: EntityDeathEvent) {
        val minionUuid = TerminatorKill.claimant(event.entity) ?: return
        val minion = PlacedMinionManager.get(minionUuid) ?: return

        val location = minion.location.toBukkit() ?: return

        val looting = minion.getUpgradeLevel(MinionUpgrade.TerminatorLooting)
        val accepted = event.drops.filter { minion.accepts(it) }.map { withLooting(it, looting) }
        val leftover = minion.addToStorage(accepted)

        event.drops.clear()

        if (minion.fullMode != FullMode.TRASH) {
            leftover.forEach { location.world.dropItemNaturally(location, it) }
        }

        val xp = if (event.droppedExp > 0) event.droppedExp
        else if (event.entity is Enemy) XP_HOSTILE
        else XP_PASSIVE

        minion.addXp(xp)
        minion.countProduced()
        event.droppedExp = 0

        PlacedMinionManager.update(minion)
    }

    private fun withLooting(drop: ItemStack, level: Int): ItemStack {
        if (level <= 0) return drop.clone()
        val bonus = Random.nextInt(level + 1)
        if (bonus <= 0) return drop.clone()
        return drop.clone().apply { amount += bonus }
    }

    private companion object {
        const val XP_HOSTILE = 5
        const val XP_PASSIVE = 1
    }
}
