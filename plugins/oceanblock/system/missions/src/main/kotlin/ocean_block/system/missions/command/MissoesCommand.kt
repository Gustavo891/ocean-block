package ocean_block.system.missions.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import ocean_block.system.missions.MissionTracker
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.data.MissionManager
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("missoes|missao")
class MissoesCommand : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        MissionTracker.refresh(player)
        summary(player).forEach(player::sendRichMessage)
    }

    @Subcommand("pular")
    @CommandPermission("oneblock.admin")
    @CommandCompletion("@players")
    fun onSkip(sender: CommandSender, target: String) {
        val player = Bukkit.getPlayerExact(target) ?: return sender.sendRichMessage("<red>Jogador offline: $target")
        MissionTracker.skip(player)
        sender.sendRichMessage("<green>Missão atual de ${player.name} pulada.")
    }

    @Subcommand("resetar")
    @CommandPermission("oneblock.admin")
    @CommandCompletion("@players")
    fun onReset(sender: CommandSender, target: String) {
        val player = Bukkit.getPlayerExact(target) ?: return sender.sendRichMessage("<red>Jogador offline: $target")
        MissionTracker.reset(player)
        sender.sendRichMessage("<green>Missões de ${player.name} resetadas.")
    }

    companion object {
        fun summary(player: Player): List<String> {
            val state = MissionManager.get(player.uniqueId)
            if (!MissionRules.trailDone(state)) {
                val current = MissionRules.current(state)
                return listOfNotNull(
                    "<aqua>Missão ${state.trailIndex + 1} de ${MissionTrail.size}",
                    current?.let { MissionText.hud(it, false) },
                    "<gray>Termine a trilha para liberar as diárias.",
                )
            }
            val dailies = state.dailies.mapIndexedNotNull { index, daily ->
                DailyPool.byId(daily.id)?.let { mission ->
                    val mark = if (daily.done) "<green>✔</green>" else "<gray>${index + 1}.</gray>"
                    val counter = MissionText.counter(mission.objective, daily.progress)
                    "$mark <white>${mission.text}</white> <gray>$counter</gray> <gold>+${daily.xp} XP"
                }
            }
            return listOf("<aqua>Trilha concluída!", "<aqua>Diárias:") + dailies
        }
    }
}
