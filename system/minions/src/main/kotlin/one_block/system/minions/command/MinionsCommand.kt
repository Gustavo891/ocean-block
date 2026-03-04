package one_block.system.minions.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.coordinate.SimpleLocation
import one_block.system.minions.enums.MinionType
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.system.minions.view.MinionsView
import org.bukkit.entity.Player
import java.util.UUID

@CommandAlias("minions")
class MinionsCommand(
    private val viewFrame: ViewFrame
): BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(MinionsView::class.java, sender)
    }
}