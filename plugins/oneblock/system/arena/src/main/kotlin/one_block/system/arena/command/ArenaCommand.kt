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
        val spawn = ArenaSystem.spawnPosition.toBukkit()
        if (spawn == null) {
            sender.sendRichMessage("<red>Erro: mundo da arena nao carregado.")
            return
        }

        sender.teleport(spawn)
    }
}
