package ocean_block.system.skills.perk

import ocean_block.system.skills.Skills
import ocean_block.system.skills.skill.SkillPerk
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import java.util.concurrent.ThreadLocalRandom

class CombatPerks : Listener {

    private companion object {

        const val EXTRA_LOOTING_CHANCE = 0.5

        const val MASSACRE_RADIUS = 4.0
        const val MASSACRE_DAMAGE = 6.0
    }

    private var inMassacre = false

    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return

        applyLooting(event, Skills.perkPower(killer, SkillPerk.ExtraLooting) * EXTRA_LOOTING_CHANCE)
        applyMassacre(event, killer, Skills.perkPower(killer, SkillPerk.Massacre) * MASSACRE_DAMAGE)
    }

    private fun applyLooting(event: EntityDeathEvent, lootChance: Double) {
        if (event.drops.isEmpty()) return
        if (!chance(lootChance)) return

        val drop = event.drops.random()
        drop.amount += 1
    }

    private fun applyMassacre(event: EntityDeathEvent, killer: Player, damage: Double) {
        if (damage <= 0.0) return
        if (inMassacre) return

        val dead = event.entity
        val nearby = dead.getNearbyEntities(MASSACRE_RADIUS, MASSACRE_RADIUS, MASSACRE_RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it !is Player && !it.isDead }

        if (nearby.isEmpty()) return

        inMassacre = true
        try {
            nearby.forEach { it.damage(damage, killer) }
        } finally {
            inMassacre = false
        }
    }

    private fun chance(probability: Double): Boolean =
        ThreadLocalRandom.current().nextDouble() < probability
}
