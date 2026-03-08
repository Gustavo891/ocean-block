package one_block.system.repair.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import one_block.system.repair.Repairs
import one_block.system.repair.enums.Fragment
import one_block.system.repair.enums.Repair
import org.bukkit.entity.Player

@CommandAlias("reparo")
@CommandPermission("screen.dev")
class RepairCommand : BaseCommand() {

    @Subcommand("tipo")
    fun onType(sender: Player, target: OnlinePlayer, repair: Repair) {
        val fragmentItem = Repairs.createRepairItem(repair)
        target.player.inventory.addItem(fragmentItem)
    }

    @Subcommand("fragmento")
    fun onFragment(sender: Player, target: OnlinePlayer, fragment: Fragment) {
        val fragmentItem = Repairs.createFragmentItem(fragment)
        target.player.inventory.addItem(fragmentItem)
    }
}