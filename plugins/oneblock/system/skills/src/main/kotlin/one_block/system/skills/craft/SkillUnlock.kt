package one_block.system.skills.craft

import one_block.system.craft.forge.RecipeUnlock
import one_block.system.skills.Skills
import one_block.system.skills.skill.Skill
import org.bukkit.entity.Player

data class SkillUnlock(
    val skill: Skill,
    val level: Int,
) : RecipeUnlock {

    override fun isUnlocked(player: Player): Boolean = Skills.hasLevel(player.uniqueId, skill, level)

    override fun requirementLines(player: Player): List<String> {
        val current = Skills.level(player.uniqueId, skill)

        return if (current >= level) {
            listOf("<dark_gray>▪ <green>✔ ${skill.prettyName} $level")
        } else {
            listOf("<dark_gray>▪ <red>✖ ${skill.prettyName} $level <dark_gray>($current)")
        }
    }
}
