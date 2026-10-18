package ocean_block.system.sell.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.sell.view.SellItemsView
import org.bukkit.entity.Player

@CommandAlias("sellitems|itensvenda|valores")
class SellItemsCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(SellItemsView::class.java, sender)
    }
}
