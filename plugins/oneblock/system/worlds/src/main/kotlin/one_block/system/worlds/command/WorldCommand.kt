package one_block.system.worlds.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import one_block.system.worlds.enums.WorldType
import one_block.system.worlds.manager.WorldManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandAlias("mundos|mundo")
@CommandPermission("screen.dev")
class WorldCommand : BaseCommand() {

    @Subcommand("ir")
    fun onDefault(sender: Player, worldId: String) {
        val world = Bukkit.getWorld(worldId)
        if (world == null) {
            sender.sendRichMessage("<red>Mundo não encontrado!")
            return
        }

        sender.teleport(world.spawnLocation)
    }

    @Subcommand("criar")
    fun onCreate(sender: Player, worldId: String, worldType: WorldType) {
        WorldManager.create(sender, worldId, worldType)
    }
}
