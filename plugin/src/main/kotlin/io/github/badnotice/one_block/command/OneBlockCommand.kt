package io.github.badnotice.one_block.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Subcommand
import one_block.system.sell.manager.SellManager
import org.bukkit.entity.Player

@CommandAlias("oneblock")
class OneBlockCommand : BaseCommand() {

    @Subcommand("reload")
    fun onReload(sender: Player) {
        SellManager.load()

        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }
}