package one_block.system.scoreboard.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import one_block.system.scoreboard.board.BoardManager
import org.bukkit.entity.Player

@CommandAlias("scoreboard|sb")
class ScoreboardCommand : BaseCommand() {

    @Default
    fun onToggle(sender: Player) {
        val visible = BoardManager.toggle(sender)

        if (visible) {
            sender.sendRichMessage("<green>Scoreboard ativada.")
            return
        }

        sender.sendRichMessage("<red>Scoreboard desativada.")
    }

    @Subcommand("reload")
    @CommandPermission("screen.dev")
    fun onReload(sender: Player) {
        BoardManager.reload()
        sender.sendRichMessage("<green>Scoreboard recarregada.")
    }
}
