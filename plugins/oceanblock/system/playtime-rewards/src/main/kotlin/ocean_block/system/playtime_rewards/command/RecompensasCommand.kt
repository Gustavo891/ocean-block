package ocean_block.system.playtime_rewards.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import ocean_block.system.playtime_rewards.PlaytimeRewards
import ocean_block.system.playtime_rewards.dialog.RewardDialog
import ocean_block.system.playtime_rewards.rules.MilestoneRules
import ocean_block.system.playtime_rewards.rules.MilestoneStatus
import ocean_block.system.playtime_rewards.text.RewardText
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("recompensas|recompensa|tempo")
class RecompensasCommand : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        if (RewardDialog.open(player)) return
        summary(player).forEach(player::sendRichMessage)
    }

    @Subcommand("resgatar")
    fun onClaim(player: Player, id: String) {
        PlaytimeRewards.claim(player, id)
    }

    @Subcommand("resetar")
    @CommandPermission("oneblock.admin")
    @CommandCompletion("@players")
    fun onReset(sender: CommandSender, target: String) {
        val player = Bukkit.getPlayerExact(target) ?: return sender.sendRichMessage("<red>Jogador offline: $target")
        PlaytimeRewards.reset(player)
        sender.sendRichMessage("<green>Recompensas por tempo de ${player.name} resetadas.")
    }

    private fun summary(player: Player): List<String> {
        val seconds = PlaytimeRewards.seconds(player) ?: return listOf("<red>Seu tempo jogado ainda não carregou.")
        val claimed = PlaytimeRewards.claimed(player)
        val lines = PlaytimeRewards.ladder.map { milestone ->
            val name = RewardText.duration(milestone.minutes)
            val prizes = RewardText.prizes(milestone, RewardDialog::itemName)
            when (val status = MilestoneRules.status(milestone, seconds, claimed)) {
                MilestoneStatus.Claimed -> "<green>✔ $name</green> <gray>$prizes"
                MilestoneStatus.Available ->
                    "<aqua>$name</aqua> <white>$prizes</white> <green><click:run_command:'/recompensas resgatar ${milestone.id}'>[RESGATAR]</click>"
                is MilestoneStatus.Locked -> "<gray>$name $prizes <dark_gray>(faltam ${RewardText.remaining(status.missingSeconds)})"
            }
        }
        return listOf("<aqua>Tempo jogado: <white>${RewardText.played(seconds)}") + lines
    }
}
