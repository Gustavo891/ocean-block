package ocean_block.system.battle_pass.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.battle_pass.view.ChallengesView
import org.bukkit.entity.Player

@CommandAlias("missao|missoes|desafios")
class MissionCommand(private val viewFrame: ViewFrame) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(ChallengesView::class.java, sender)
    }

}
