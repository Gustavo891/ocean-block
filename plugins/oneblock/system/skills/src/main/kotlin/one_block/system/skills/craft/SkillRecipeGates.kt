package one_block.system.skills.craft

import one_block.system.craft.Crafts
import one_block.system.skills.skill.Skill

object SkillRecipeGates {

    fun registerAll() {
        Crafts.gate("platinum_ingot", SkillUnlock(Skill.Mining, 15))
        Crafts.gate("miner_minion", SkillUnlock(Skill.Mining, 30))
        Crafts.gate("vacuum_hopper", SkillUnlock(Skill.Mining, 40))

        Crafts.gate("farmer_minion", SkillUnlock(Skill.Farming, 30))

        Crafts.gate("terminator_minion", SkillUnlock(Skill.Combat, 30))
        Crafts.gate("repair_2", SkillUnlock(Skill.Combat, 20))
        Crafts.gate("repair_3", SkillUnlock(Skill.Combat, 45))
    }
}
