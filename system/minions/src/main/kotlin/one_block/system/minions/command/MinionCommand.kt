package one_block.system.minions.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import one_block.libs.coordinate.SimpleLocation
import one_block.system.minions.enums.MinionType
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.entity.Player
import java.util.UUID

@CommandAlias("minion")
@CommandPermission("screen.dev")
class MinionCommand : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        val placedMinion = PlacedMinion(UUID.randomUUID(), SimpleLocation.fromBukkit(sender.location), MinionType.Farmer, 0)
        MinionType.Farmer.spawn(sender.location)

        PlacedMinionManager.register(placedMinion)
    }
}