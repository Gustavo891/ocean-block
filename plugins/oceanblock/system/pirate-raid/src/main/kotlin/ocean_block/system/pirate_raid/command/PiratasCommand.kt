package ocean_block.system.pirate_raid.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.level.PlayerLevels
import ocean_block.system.pirate_raid.raid.RaidDirector
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("piratas")
@CommandPermission("oneblock.admin")
class PiratasCommand : BaseCommand() {

    @Subcommand("iniciar")
    @CommandCompletion("@players")
    fun onStart(sender: CommandSender, @Optional target: String?) {
        val player = player(sender, target) ?: return
        val island = IslandManager.getIslandAt(player.location)
            ?: return sender.sendRichMessage("<red>${player.name} precisa estar dentro de uma ilha.")
        val level = RaidDirector.levelOf(island).takeIf { it > 0 } ?: PlayerLevels.levelOf(player)
        if (!RaidDirector.begin(island, level)) return sender.sendRichMessage("<red>Essa ilha já está sob ataque.")
        sender.sendRichMessage("<green>Raid pirata iniciada na ilha de ${island.ownerName} (nível $level).")
    }

    @Subcommand("parar")
    @CommandCompletion("@players")
    fun onStop(sender: CommandSender, @Optional target: String?) {
        val player = player(sender, target) ?: return
        val island = IslandManager.getIslandAt(player.location)
            ?: return sender.sendRichMessage("<red>${player.name} precisa estar dentro de uma ilha.")
        if (!RaidDirector.stop(island.uuid)) return sender.sendRichMessage("<red>Essa ilha não está sob ataque.")
        sender.sendRichMessage("<green>Raid pirata encerrada.")
    }

    private fun player(sender: CommandSender, target: String?): Player? {
        if (target == null) return (sender as? Player) ?: null.also { sender.sendRichMessage("<red>Informe um jogador.") }
        return Bukkit.getPlayerExact(target) ?: null.also { sender.sendRichMessage("<red>Jogador offline: $target") }
    }
}
