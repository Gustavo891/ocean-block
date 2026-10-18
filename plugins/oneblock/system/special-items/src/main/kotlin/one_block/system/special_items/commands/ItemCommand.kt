package one_block.system.special_items.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import one_block.system.special_items.sell.SellBeaconManager
import org.bukkit.entity.Player

@CommandAlias("beacon")
@CommandPermission("screen.dev")
class ItemCommand() : BaseCommand() {

    @Default
    fun onDefault(sender: Player, @Optional targetName: String?) {
        val item = SellBeaconManager.createBeaconItem(null)
        sender.inventory.addItem(item)
    }
}
