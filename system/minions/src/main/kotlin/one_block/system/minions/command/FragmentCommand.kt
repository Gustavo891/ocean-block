package one_block.system.minions.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import one_block.system.minions.Minions
import org.bukkit.entity.Player

@CommandAlias("fragments|fragment|fra")
@CommandPermission("screen.dev")
class FragmentCommand: BaseCommand() {

    @Default
    fun onDefault(sender: Player, amount: Int) {
        val fuelItem = Minions.machinePart()
        sender.inventory.addItem(fuelItem)

        sender.sendMessage("Comando fragmento = ${amount}")
    }
}