package one_block.system.spawn.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import one_block.system.spawn.spawn.SpawnManager
import org.bukkit.entity.Player

@CommandAlias("spawn")
class SpawnCommand : BaseCommand() {

    @Default
    fun onSpawn(sender: Player) {
        if (!SpawnManager.isDefined()) {
            sender.sendRichMessage("<red>O spawn ainda não foi definido!")
            return
        }

        if (!SpawnManager.teleport(sender)) {
            sender.sendRichMessage("<red>O mundo do spawn não está carregado!")
            return
        }

        sender.sendRichMessage("<green>Você foi teleportado para o spawn.")
    }
}
