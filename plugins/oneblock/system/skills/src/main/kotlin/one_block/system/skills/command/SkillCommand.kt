package one_block.system.skills.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.skills.Skills
import one_block.system.skills.skill.Skill
import one_block.system.skills.view.SkillsView
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("skills|habilidades")
class SkillCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(SkillsView::class.java, sender)
    }

    @Subcommand("setlevel")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players @skills @nothing")
    @Syntax("<jogador> <skill> <nivel>")
    fun onSetLevel(sender: CommandSender, onlineTarget: OnlinePlayer, skillId: String, level: Int) {
        val skill = resolve(sender, skillId) ?: return
        val target = requireLoaded(sender, onlineTarget) ?: return

        Skills.setLevel(target.uniqueId, skill, level)

        val applied = Skills.level(target.uniqueId, skill)
        sender.sendRichMessage("<green>${skill.prettyName} de ${target.name} definida no nível <white>$applied<green>.")
    }

    @Subcommand("addxp")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players @skills @nothing")
    @Syntax("<jogador> <skill> <xp>")
    fun onAddXp(sender: CommandSender, onlineTarget: OnlinePlayer, skillId: String, amount: Long) {
        val skill = resolve(sender, skillId) ?: return
        val target = requireLoaded(sender, onlineTarget) ?: return

        val before = Skills.level(target.uniqueId, skill)
        Skills.addXp(target.uniqueId, skill, amount)
        val after = Skills.level(target.uniqueId, skill)

        sender.sendRichMessage("<green>Adicionado $amount de XP em ${skill.prettyName} para ${target.name}.")
        if (after > before) {
            sender.sendRichMessage("<dark_gray>▸ <white>Nível $before <dark_gray>» <white>$after")
        }
    }

    @Subcommand("reset")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players @skills")
    @Syntax("<jogador> [skill]")
    fun onReset(sender: CommandSender, onlineTarget: OnlinePlayer, @Optional skillId: String?) {
        val target = requireLoaded(sender, onlineTarget) ?: return

        if (skillId == null) {
            Skills.resetAll(target.uniqueId)
            sender.sendRichMessage("<green>Todas as habilidades de ${target.name} foram resetadas.")
            return
        }

        val skill = resolve(sender, skillId) ?: return

        Skills.reset(target.uniqueId, skill)
        sender.sendRichMessage("<green>${skill.prettyName} de ${target.name} foi resetada.")
    }

    private fun resolve(sender: CommandSender, skillId: String): Skill? {
        val skill = Skill.byId(skillId)
        if (skill == null) {
            sender.sendRichMessage("<red>Skill inválida. Use: ${Skill.entries.joinToString { it.id }}")
        }
        return skill
    }

    private fun requireLoaded(sender: CommandSender, onlineTarget: OnlinePlayer): Player? {
        val target = onlineTarget.player
        if (Skills.data(target.uniqueId) == null) {
            sender.sendRichMessage("<red>Dados de skill do jogador não carregados.")
            return null
        }
        return target
    }
}
