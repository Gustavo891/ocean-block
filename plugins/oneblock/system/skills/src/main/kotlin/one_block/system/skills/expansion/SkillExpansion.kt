package one_block.system.skills.expansion

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import one_block.libs.formatter.NumberFormatter
import one_block.system.skills.skill.Skill
import one_block.system.skills.skill.SkillManager
import org.bukkit.entity.Player

class SkillExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String = "skill"
    override fun getAuthor(): String = "ocoisaruim"
    override fun getVersion(): String = "1.0"
    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val parts = params.split("_")
        if (parts.size < 2) return null

        val skill = Skill.byId(parts[0]) ?: return null
        val data = SkillManager.get(player.uniqueId) ?: return "0"

        return when (parts[1].lowercase()) {
            "level" -> data.levelOf(skill).toString()
            "xp" -> NumberFormatter.format(data.xpOf(skill).toDouble())
            "progress" -> NumberFormatter.format(data.progressOf(skill).toDouble())
            "next" -> NumberFormatter.format(data.nextLevelCost(skill).toDouble())
            "bonus" -> "${(data.bonusOf(skill) * 100).toInt()}%"
            else -> null
        }
    }
}
