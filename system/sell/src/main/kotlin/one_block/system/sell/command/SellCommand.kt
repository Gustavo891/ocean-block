package one_block.system.sell.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.currencies.Currencies
import one_block.system.sell.Sell
import one_block.system.sell.SellSystem
import one_block.system.sell.manager.SellManager
import one_block.system.sell.view.SellView
import org.bukkit.entity.Player

@CommandAlias("sell|vender")
class SellCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        val itemInMainHand = sender.inventory.itemInMainHand
        if (itemInMainHand.isEmpty) {
            sender.sendRichMessage("<color:#b00f00>Você não pode vender sua mão né bobinho....")
            return
        }

        val sellHand = Sell.sellHand(sender)
        if (sellHand == 0.0) {
            sender.sendRichMessage("<color:#b00f00>Você não pode vender este item.")
            return
        }

        Currencies.deposit(sender.uniqueId, "coins", sellHand)
        sender.sendRichMessage("<green>Você recebeu <green>$<white>${sellHand}</white></green> com suas vendas.")
    }

    @Subcommand("item")
    @CommandPermission("screen.dev")
    fun onItem(sender: Player, target: OnlinePlayer, uses: Int) {
        val createSellItem = Sell.createSellItem(uses)
        target.player.inventory.addItem(createSellItem)

        sender.sendRichMessage("<green>Deu o item ggggggggggggg")
    }

    @Subcommand("gui")
    fun onGui(sender: Player) {
        viewFrame.open(SellView::class.java, sender)
    }

    @Subcommand("reload")
    @CommandPermission("screen.dev")
    fun onReload(sender: Player) {
        SellManager.load()
        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }

}