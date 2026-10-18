package one_block.system.skills.craft

import one_block.system.craft.Crafts
import one_block.system.skills.skill.Skill

object SkillRecipeGates {

    fun registerAll() {
        Crafts.gate("platinum_ingot", SkillUnlock(Skill.Mining, 15))
    }
}
