package one_block.system.minions.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import one_block.libs.coordinate.SimpleLocation
import one_block.system.minions.Minions
import one_block.system.minions.enums.MinionType
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

@CommandAlias("minion")
@CommandPermission("screen.dev")
class MinionCommand : BaseCommand() {

    @Subcommand("give")
    fun onGive(sender: Player, typeName: String) {
        val type = runCatching { MinionType.valueOf(typeName) }.getOrElse {
            sender.sendMessage("Tipo inválido. Tipos: ${MinionType.entries.joinToString()}")
            return
        }
        sender.inventory.addItem(Minions.createMinionItem(type))
        sender.sendMessage("Minion ${type.name} dado para ${sender.name}.")
    }
}