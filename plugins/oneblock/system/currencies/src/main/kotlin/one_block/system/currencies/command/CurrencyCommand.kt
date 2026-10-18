package one_block.system.currencies.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import one_block.libs.formatter.NumberFormatter
import one_block.system.currencies.wallet.Wallet
import one_block.system.currencies.wallet.WalletManager
import org.bukkit.entity.Player

class CurrencyCommand(
    private val walletManager: WalletManager,
) : BaseCommand(walletManager.currency.id) {

    private val currencyName = walletManager.currency.prettyName

    @Default
    fun onDefault(sender: Player) {
        val wallet = walletManager.find(sender.uniqueId)
        if (wallet == null) {
            sender.sendRichMessage("<red>Sua carteira não foi carregada!")
            return
        }

        sender.sendRichMessage("<white>Saldo: <yellow>${format(wallet)}")
    }

    @Subcommand("ver")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players")
    fun onSee(sender: Player, targetName: String) {
        val wallet = resolve(sender, targetName) ?: return

        sender.sendRichMessage("<white>Saldo de <yellow>${wallet.name}<white>: <yellow>${format(wallet)}")
    }

    @Subcommand("add")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players")
    fun onAdd(sender: Player, targetName: String, rawValue: String) {
        val wallet = resolve(sender, targetName) ?: return
        val value = parsePositive(sender, rawValue) ?: return

        walletManager.deposit(wallet, value)

        sender.sendRichMessage(
            "<green>Adicionado <white>${NumberFormatter.format(value)} $currencyName</white> " +
                "para <white>${wallet.name}</white>. <gray>(saldo: ${format(wallet)})"
        )
    }

    @Subcommand("remover|remove|take")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players")
    fun onRemove(sender: Player, targetName: String, rawValue: String) {
        val wallet = resolve(sender, targetName) ?: return
        val value = parsePositive(sender, rawValue) ?: return

        val before = wallet.balance
        walletManager.setBalance(wallet, before - value)
        val taken = before - wallet.balance

        sender.sendRichMessage(
            "<green>Removido <white>${NumberFormatter.format(taken)} $currencyName</white> " +
                "de <white>${wallet.name}</white>. <gray>(saldo: ${format(wallet)})"
        )

        if (taken < value) {
            sender.sendRichMessage("<yellow>O jogador tinha menos que isso; o saldo foi zerado.")
        }
    }

    @Subcommand("set|definir")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players")
    fun onSet(sender: Player, targetName: String, rawValue: String) {
        val wallet = resolve(sender, targetName) ?: return
        val value = parseAmount(rawValue)

        if (value == null || value < 0.0) {
            sender.sendRichMessage("<red>Valor inválido: <white>$rawValue</white>")
            return
        }

        walletManager.setBalance(wallet, value)

        sender.sendRichMessage(
            "<green>Saldo de <white>${wallet.name}</white> definido para <white>${format(wallet)}</white>."
        )
    }

    @Subcommand("pagar|pay")
    @CommandCompletion("@players")
    fun onPay(sender: Player, target: OnlinePlayer, rawValue: String) {
        if (target.player.uniqueId == sender.uniqueId) {
            sender.sendRichMessage("<red>Você não pode pagar a si mesmo.")
            return
        }

        val senderWallet = walletManager.find(sender.uniqueId)
        if (senderWallet == null) {
            sender.sendRichMessage("<red>Sua carteira não foi carregada!")
            return
        }

        val targetWallet = walletManager.find(target.player.uniqueId)
        if (targetWallet == null) {
            sender.sendRichMessage("<red>A carteira do jogador ${target.player.name} não foi carregada!")
            return
        }

        val value = parsePositive(sender, rawValue) ?: return

        if (!walletManager.withdraw(senderWallet, value)) {
            sender.sendRichMessage("<red>Você não possui esse dinheiro!")
            return
        }

        walletManager.deposit(targetWallet, value)

        val formatted = "${NumberFormatter.format(value)} $currencyName"
        sender.sendRichMessage("<green>Você pagou <white>$formatted</white> para <white>${target.player.name}</white>")
        target.player.sendRichMessage("<green><white>${sender.name}</white> te enviou <white>$formatted</white>")
    }

    private fun resolve(sender: Player, targetName: String): Wallet? {
        val wallet = walletManager.find(targetName)

        if (wallet == null) {
            sender.sendRichMessage(
                "<red>Carteira de <white>$targetName</white> não encontrada. " +
                    "<gray>Ele precisa ter entrado no servidor desde o último restart."
            )
        }

        return wallet
    }

    private fun parsePositive(sender: Player, rawValue: String): Double? {
        val value = parseAmount(rawValue)

        if (value == null || value <= 0.0) {
            sender.sendRichMessage("<red>Valor inválido: <white>$rawValue</white>")
            return null
        }

        return value
    }

    private fun parseAmount(rawValue: String): Double? {
        val value = NumberFormatter.deform(rawValue)
        if (value == 0.0 && rawValue.trim().toDoubleOrNull() != 0.0) return null

        return value
    }

    private fun format(wallet: Wallet): String = "${NumberFormatter.format(wallet.balance)} $currencyName"
}
