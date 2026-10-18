package ocean_block.system.skills.craft

import ocean_block.system.craft.Crafts
import ocean_block.system.skills.skill.Skill

object SkillRecipeGates {

    fun registerAll() {
        Crafts.gate("platinum_ingot", SkillUnlock(Skill.Mining, 15))
    }
}
