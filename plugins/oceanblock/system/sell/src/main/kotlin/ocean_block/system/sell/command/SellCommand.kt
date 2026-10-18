package ocean_block.system.sell.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.sell.Sell
import ocean_block.system.sell.manager.SellCooldown
import ocean_block.system.sell.manager.SellManager
import ocean_block.system.sell.view.SellView
import org.bukkit.Sound
import org.bukkit.entity.Player

@CommandAlias("sell|vender")
class SellCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    companion object {
        private const val INVENTORY_COOLDOWN_MILLIS = 10_000L
        private const val INVENTORY_PERMISSION = "sell.inventory"
    }

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(SellView::class.java, sender)
    }

    @Subcommand("inventario|inventory|inv")
    @CommandPermission(INVENTORY_PERMISSION)
    fun onInventory(sender: Player) {
        val remaining = SellCooldown.remainingSeconds(sender.uniqueId)
        if (remaining > 0) {
            sender.sendRichMessage("<color:#b00f00>Aguarde <white>${remaining}s<color:#b00f00> para vender seu inventário novamente.")
            return
        }

        val totalMoney = Sell.sellInventory(sender)
        if (totalMoney <= 0.0) {
            sender.sendRichMessage("<color:#b00f00>Nenhum item vendável encontrado no seu inventário.")
            return
        }

        SellCooldown.stamp(sender.uniqueId, INVENTORY_COOLDOWN_MILLIS)
        sender.sendRichMessage("<green>Você recebeu <dark_green>$<white>${NumberFormatter.format(totalMoney)}</white> <green>com suas vendas!")
        sender.playSound(sender, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
    }

    @Subcommand("item")
    @CommandPermission("screen.dev")
    fun onItem(sender: Player, target: OnlinePlayer, uses: Int) {
        val createSellItem = Sell.createSellItem(uses)
        target.player.inventory.addItem(createSellItem)

        sender.sendRichMessage("<green>Deu o item ggggggggggggg")
    }

    @Subcommand("reload")
    @CommandPermission("screen.dev")
    fun onReload(sender: Player) {
        SellManager.load()
        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }

}
