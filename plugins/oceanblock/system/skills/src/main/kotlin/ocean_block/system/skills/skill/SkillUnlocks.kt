package ocean_block.system.skills.skill

import ocean_block.system.craft.forge.ForgeRecipeManager
import ocean_block.system.skills.craft.SkillUnlock

data class SkillUnlockInfo(
    val skill: Skill,
    val level: Int,
    val prettyName: String,
    val description: String? = null,
    val craft: Boolean = false,
)

object SkillUnlocks {

    private val manual = mutableListOf<SkillUnlockInfo>()

    fun register(info: SkillUnlockInfo) {
        manual += info
    }

    fun at(skill: Skill, level: Int): List<SkillUnlockInfo> =
        manual.filter { it.skill == skill && it.level == level } + recipesAt(skill, level)

    fun nextAfter(skill: Skill, fromLevel: Int): Int? =
        ((fromLevel + 1)..Skill.MAX_LEVEL).firstOrNull { at(skill, it).isNotEmpty() }

    private fun recipesAt(skill: Skill, level: Int): List<SkillUnlockInfo> =
        ForgeRecipeManager.recipes.mapNotNull { recipe ->
            val unlock = ForgeRecipeManager.unlockOf(recipe.id) as? SkillUnlock ?: return@mapNotNull null
            if (unlock.skill != skill || unlock.level != level) return@mapNotNull null

            SkillUnlockInfo(
                skill = skill,
                level = level,
                prettyName = recipe.prettyName,
                description = "Receita liberada na forja.",
                craft = true,
            )
        }
}
