package ocean_block.system.level.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.level.PlayerLevels
import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.level.data.LevelManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("nivel|level|niveis")
class NivelCommand : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        summary(player).forEach(player::sendRichMessage)
    }

    @Subcommand("dar")
    @CommandPermission("oneblock.admin")
    @CommandCompletion("@players")
    fun onGive(sender: CommandSender, target: String, xp: Long) {
        val player = Bukkit.getPlayerExact(target) ?: return sender.sendRichMessage("<red>Jogador offline: $target")
        PlayerLevels.setXp(player, LevelManager.xp(player.uniqueId) + xp)
        sender.sendRichMessage("<green>$xp XP para ${player.name}. Nível ${PlayerLevels.levelOf(player)}.")
    }

    @Subcommand("definir")
    @CommandPermission("oneblock.admin")
    @CommandCompletion("@players")
    fun onSet(sender: CommandSender, target: String, level: Int) {
        val player = Bukkit.getPlayerExact(target) ?: return sender.sendRichMessage("<red>Jogador offline: $target")
        PlayerLevels.setXp(player, LevelCurve.totalFor(level.coerceIn(1, LevelCurve.MAX_LEVEL)))
        sender.sendRichMessage("<green>${player.name} agora está no nível ${PlayerLevels.levelOf(player)}.")
    }

    companion object {
        fun summary(player: Player): List<String> {
            val xp = LevelManager.xp(player.uniqueId)
            val level = LevelCurve.levelOf(xp)
            val next = Recipes.all().filter { it.level > level }.minByOrNull { it.level }
            val progress = if (level >= LevelCurve.MAX_LEVEL) "Nível máximo"
            else "${xp - LevelCurve.totalFor(level)} / ${LevelCurve.toNext(level)} XP"
            return listOfNotNull(
                "<aqua>Nível $level</aqua> <gray>$progress",
                next?.let { "<gray>Próxima receita: <white>${it.name}</white> no nível ${it.level}" },
            )
        }
    }
}
