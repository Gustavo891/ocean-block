package one_block.system.shop.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.shop.shop.ShopManager
import one_block.system.shop.view.ShopView
import org.bukkit.entity.Player

@CommandAlias("shop")
class ShopCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(ShopView::class.java, sender)
    }

    @Subcommand("reload")
    @CommandPermission("screen.dev")
    fun onReload(sender: Player) {
        ShopManager.load()
        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }
}