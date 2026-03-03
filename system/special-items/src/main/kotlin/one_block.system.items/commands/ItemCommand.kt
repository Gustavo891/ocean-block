package one_block.system.items.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import one_block.system.items.sell.SellBeaconManager
import org.bukkit.entity.Player

@CommandAlias("beacon")
class ItemCommand() : BaseCommand() {

    @Default
    fun onDefault(sender: Player, @Optional targetName: String?) {
        val item = SellBeaconManager.createBeaconItem()
        sender.inventory.addItem(item)
    }
}