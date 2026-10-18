package one_block.system.skills.skill

import java.util.UUID

data class SkillProgress(
    var level: Int = 0,
    var currentXp: Long = 0,
    var rewardedLevel: Int = 0,
)

data class SkillData(
    val uuid: UUID,
    val name: String,
    var skills: MutableMap<String, SkillProgress>? = null,
    val xp: MutableMap<String, Long> = mutableMapOf(),
    var rewardedLevels: MutableMap<String, Int>? = null,
    var levels: MutableMap<String, Int>? = null,
    var currentXp: MutableMap<String, Long>? = null,
) {

    private fun peek(skill: Skill): SkillProgress? = skills?.get(skill.id)

    private fun entry(skill: Skill): SkillProgress {
        val map = skills ?: mutableMapOf<String, SkillProgress>().also { skills = it }
        return map.getOrPut(skill.id) { SkillProgress() }
    }

    fun levelOf(skill: Skill): Int = peek(skill)?.level ?: 0

    fun progressOf(skill: Skill): Long = peek(skill)?.currentXp ?: 0L

    fun xpOf(skill: Skill): Long = Skill.totalXpForLevel(levelOf(skill)) + progressOf(skill)

    fun bonusOf(skill: Skill): Double = skill.bonusAt(levelOf(skill))

    fun nextLevelCost(skill: Skill): Long = Skill.xpForLevel(levelOf(skill) + 1)

    fun rewardedLevelOf(skill: Skill): Int = peek(skill)?.rewardedLevel ?: 0

    fun markRewarded(skill: Skill, level: Int) {
        val progress = entry(skill)
        progress.rewardedLevel = maxOf(progress.rewardedLevel, level)
    }

    fun addXp(skill: Skill, amount: Long): Int {
        if (amount <= 0) return 0

        val progress = entry(skill)
        val before = progress.level
        if (before >= Skill.MAX_LEVEL) return 0

        var level = before
        var current = progress.currentXp + amount

        while (level < Skill.MAX_LEVEL) {
            val cost = Skill.xpForLevel(level + 1)
            if (current < cost) break

            current -= cost
            level++
        }

        if (level >= Skill.MAX_LEVEL) current = 0

        progress.level = level
        progress.currentXp = current

        return level - before
    }

    fun setLevel(skill: Skill, level: Int) {
        val progress = entry(skill)
        progress.level = level.coerceIn(0, Skill.MAX_LEVEL)
        progress.currentXp = 0
    }

    fun setXp(skill: Skill, total: Long) {
        val safe = total.coerceAtLeast(0)
        val progress = entry(skill)

        progress.level = Skill.levelForXp(safe)
        progress.currentXp = Skill.xpIntoLevel(safe)
    }

    fun reset(skill: Skill) {
        skills?.remove(skill.id)
    }

    fun resetAll() {
        skills?.clear()
    }

    fun migrateLegacy(): Boolean {
        if (skills != null) return false

        val legacyLevels = levels
        if (legacyLevels == null && xp.isEmpty()) return false

        val migrated = mutableMapOf<String, SkillProgress>()

        if (legacyLevels != null) {
            for (skill in Skill.entries) {
                val level = legacyLevels[skill.id] ?: 0
                val current = currentXp?.get(skill.id) ?: 0L
                val rewarded = rewardedLevels?.get(skill.id) ?: 0
                if (level == 0 && current == 0L && rewarded == 0) continue

                migrated[skill.id] = SkillProgress(level, current, rewarded)
            }
        } else {
            for ((id, total) in xp) {
                if (Skill.byId(id) == null) continue

                migrated[id] = SkillProgress(
                    level = Skill.levelForXp(total),
                    currentXp = Skill.xpIntoLevel(total),
                    rewardedLevel = rewardedLevels?.get(id) ?: 0,
                )
            }
        }

        skills = migrated
        xp.clear()
        levels = null
        currentXp = null
        rewardedLevels = null

        return true
    }
}
