package one_block.system.skills

import one_block.system.skills.feedback.XpFeedback
import one_block.system.skills.reward.SkillRewarder
import one_block.system.skills.skill.Skill
import one_block.system.skills.skill.SkillData
import one_block.system.skills.skill.SkillManager
import one_block.system.skills.skill.SkillPerk
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

object Skills {

    fun data(uuid: UUID): SkillData? = SkillManager.get(uuid)

    fun level(uuid: UUID, skill: Skill): Int = data(uuid)?.levelOf(skill) ?: 0

    fun xp(uuid: UUID, skill: Skill): Long = data(uuid)?.xpOf(skill) ?: 0L

    fun bonus(uuid: UUID, skill: Skill): Double = data(uuid)?.bonusOf(skill) ?: 0.0

    fun hasLevel(uuid: UUID, skill: Skill, level: Int): Boolean = level(uuid, skill) >= level

    fun perkPower(uuid: UUID, perk: SkillPerk): Double = perk.valueAt(level(uuid, perk.skill))

    fun perkPower(player: Player, perk: SkillPerk): Double = perkPower(player.uniqueId, perk)

    fun hasPerk(uuid: UUID, perk: SkillPerk): Boolean = perkPower(uuid, perk) > 0.0

    fun hasPerk(player: Player, perk: SkillPerk): Boolean = hasPerk(player.uniqueId, perk)

    fun addXp(uuid: UUID, skill: Skill, amount: Long, at: Location? = null) {
        val data = data(uuid) ?: return
        val gained = data.addXp(skill, amount)
        SkillManager.markDirty(uuid)

        val player = Bukkit.getPlayer(uuid) ?: return
        XpFeedback.record(player, skill, amount, at)

        if (gained <= 0) return

        val level = data.levelOf(skill)

        announceLevelUp(player, skill, level)
        announcePerks(player, skill, level, gained)
        SkillRewarder.grantUpTo(player, data, skill)
    }

    fun setXp(uuid: UUID, skill: Skill, amount: Long) {
        val data = data(uuid) ?: return
        data.setXp(skill, amount)
        SkillManager.markDirty(uuid)
    }

    fun setLevel(uuid: UUID, skill: Skill, level: Int) {
        val data = data(uuid) ?: return
        data.setLevel(skill, level)
        SkillManager.markDirty(uuid)
    }

    fun reset(uuid: UUID, skill: Skill) {
        val data = data(uuid) ?: return
        data.reset(skill)
        SkillManager.markDirty(uuid)
    }

    fun resetAll(uuid: UUID) {
        val data = data(uuid) ?: return
        data.resetAll()
        SkillManager.markDirty(uuid)
    }

    private fun announceLevelUp(player: Player, skill: Skill, level: Int) {
        val percent = (skill.bonusAt(level) * 100).toInt()

        player.sendRichMessage("<green>${skill.prettyName} subiu para o nível <white>$level<green>!")
        player.sendRichMessage("<dark_gray>▸ <white>+$percent% de ${skill.bonusDescription}")
    }

    private fun announcePerks(player: Player, skill: Skill, level: Int, gained: Int) {
        val from = (level - gained + 1).coerceAtLeast(1)

        for (crossed in from..level) {
            for (unlock in SkillPerk.unlockedAt(skill, crossed)) {
                val name = "${unlock.perk.prettyName} ${unlock.tier.roman}"

                player.sendRichMessage("")
                player.sendRichMessage("<gold>Perk desbloqueado: <white>$name <gray>(${unlock.tier.percent}%)")
                player.sendRichMessage("<dark_gray>▸ <gray>${unlock.perk.description}")
                player.sendRichMessage("")
            }
        }
    }
}
