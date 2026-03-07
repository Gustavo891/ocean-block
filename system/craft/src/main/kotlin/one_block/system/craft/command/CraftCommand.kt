package one_block.system.craft.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.craft.view.CraftView
import org.bukkit.entity.Player

@CommandAlias("craft")
class CraftCommand(
    private val viewFrame: ViewFrame
): BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(CraftView::class.java, sender)
    }
}