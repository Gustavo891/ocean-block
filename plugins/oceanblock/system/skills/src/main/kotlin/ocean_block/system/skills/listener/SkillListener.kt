package ocean_block.system.skills.listener

import ocean_block.system.skills.Skills
import ocean_block.system.skills.skill.Skill
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.attribute.Attribute
import org.bukkit.block.data.Ageable
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerFishEvent

class SkillListener : Listener {

    private companion object {

        const val BASE_MOB_HEALTH = 20.0

        const val HEALTH_PER_BONUS_XP = 4.0
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val uuid = event.player.uniqueId
        val block = event.block

        val skill = when {
            isMatureCrop(block.type, block.blockData) -> Skill.Farming
            else -> Skill.Mining
        }

        Skills.addXp(uuid, skill, skill.xpPerAction, block.location.toCenterLocation())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        Skills.addXp(
            killer.uniqueId,
            Skill.Combat,
            combatXp(event.entity),
            event.entity.location.clone().add(0.0, event.entity.height / 2, 0.0),
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return

        Skills.addXp(
            event.player.uniqueId,
            Skill.Fishing,
            Skill.Fishing.xpPerAction,
            event.caught?.location ?: event.hook.location,
        )
    }

    private fun combatXp(entity: LivingEntity): Long {
        val base = Skill.Combat.xpPerAction
        val maxHealth = entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: BASE_MOB_HEALTH
        val bonus = ((maxHealth - BASE_MOB_HEALTH) / HEALTH_PER_BONUS_XP).toLong().coerceAtLeast(0)

        return base + bonus
    }

    private fun isMatureCrop(material: Material, data: org.bukkit.block.data.BlockData): Boolean {
        if (Tag.CROPS.isTagged(material)) {
            val ageable = data as? Ageable ?: return true
            return ageable.age >= ageable.maximumAge
        }

        return material == Material.SUGAR_CANE ||
            material == Material.MELON ||
            material == Material.PUMPKIN ||
            material == Material.BAMBOO ||
            material == Material.CACTUS
    }
}
