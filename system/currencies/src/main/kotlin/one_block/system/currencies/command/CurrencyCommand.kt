package one_block.system.currencies.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import one_block.system.currencies.wallet.WalletManager
import org.bukkit.entity.Player

class CurrencyCommand(
    private val walletManager: WalletManager,
) : BaseCommand(walletManager.currency.id) {

    @Default
    fun onDefault(sender: Player) {
        val wallet = walletManager.find(sender.uniqueId)
        if (wallet == null) {
            sender.sendRichMessage("<red>Wallet not found!")
            return
        }

        sender.sendRichMessage("<white>Saldo: ${wallet.balance}")
    }

    @Subcommand("add")
    @CommandPermission("screen.dev")
    fun onAdd(sender: Player, target: OnlinePlayer, value: Double) {
        val wallet = walletManager.find(sender.uniqueId)
        if (wallet == null) {
            sender.sendRichMessage("<red>Wallet not found!")
            return
        }

        wallet.balance += value
        sender.sendRichMessage("<green>Adicionado <white>$value</white> para o <white>${target.player.name}</white>")
    }
}