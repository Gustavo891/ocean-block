package ocean_block.system.skills.view

import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.skills.skill.Skill
import ocean_block.system.skills.skill.SkillData

object SkillProgress {

    private const val BAR_LENGTH = 10

    fun line(data: SkillData, skill: Skill): String {
        if (data.levelOf(skill) >= Skill.MAX_LEVEL) return "<green>■■■■■■■■■■ <gray>100%"

        val current = data.progressOf(skill)
        val needed = data.nextLevelCost(skill)
        val filled = ((current.toDouble() / needed) * BAR_LENGTH).toInt().coerceIn(0, BAR_LENGTH)

        val bar = buildString {
            append("<green>")
            repeat(filled) { append("■") }
            append("<dark_gray>")
            repeat(BAR_LENGTH - filled) { append("■") }
        }

        val currentText = NumberFormatter.format(current.toDouble())
        val neededText = NumberFormatter.format(needed.toDouble())

        return "$bar <gray>[$currentText/$neededText]"
    }
}
