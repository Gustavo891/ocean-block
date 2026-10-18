package ocean_block.system.tops.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.tops.view.TopsView
import org.bukkit.entity.Player

@CommandAlias("tops")
class TopsCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(TopsView::class.java, sender)
    }
}
