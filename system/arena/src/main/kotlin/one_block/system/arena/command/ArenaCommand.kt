package one_block.system.arena.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import one_block.system.arena.ArenaSystem
import org.bukkit.entity.Player

@CommandAlias("arena")
class ArenaCommand : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        sender.teleport(ArenaSystem.spawnPosition.toBukkit()!!)
    }
}