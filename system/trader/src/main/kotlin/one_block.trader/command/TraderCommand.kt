package one_block.trader.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import one_block.system.islands.island.IslandManager
import one_block.trader.manager.TraderManager
import org.bukkit.entity.Player

@CommandAlias("trader")
@CommandPermission("screen.dev")
class TraderCommand : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        val playerIsland = IslandManager.getIslandByPlayer(sender.uniqueId) ?: return
        TraderManager.spawnTrader(playerIsland)

    }

    @Subcommand("test")
    fun onTest(sender: Player) {
        sender.sendRichMessage("<#f5f528>testando123</#f5f528>")
    }
}