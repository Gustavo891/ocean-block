package one_block.system.mobdrops.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import one_block.system.mobdrops.manager.MobDropManager
import org.bukkit.entity.Player

@CommandAlias("mobdrops|md")
@CommandPermission("screen.dev")
class MobDropsCommand : BaseCommand() {

    @Subcommand("reload")
    @CommandPermission("screen.dev")
    fun onReload(sender: Player) {
        MobDropManager.load()
        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }
}
