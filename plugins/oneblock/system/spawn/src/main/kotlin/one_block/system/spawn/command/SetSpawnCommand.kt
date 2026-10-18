package one_block.system.spawn.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import one_block.system.spawn.spawn.SpawnManager
import one_block.system.spawn.world.SpawnWorldRules
import org.bukkit.entity.Player

@CommandAlias("setspawn")
@CommandPermission("screen.dev")
class SetSpawnCommand : BaseCommand() {

    @Default
    fun onSetSpawn(sender: Player) {
        SpawnManager.set(sender.location)
        SpawnWorldRules.apply(sender.world)

        val world = sender.world.name
        sender.sendRichMessage("<green>Spawn definido em <white>$world <gray>(${sender.location.blockX}, ${sender.location.blockY}, ${sender.location.blockZ})<green>.")
    }
}
