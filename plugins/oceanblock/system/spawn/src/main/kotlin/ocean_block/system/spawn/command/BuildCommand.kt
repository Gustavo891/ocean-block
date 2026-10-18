package ocean_block.system.spawn.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import ocean_block.system.spawn.build_mode.BuildMode
import org.bukkit.entity.Player

@CommandAlias("build|construir")
@CommandPermission(BuildMode.PERMISSION)
class BuildCommand : BaseCommand() {

    @Default
    fun onToggle(sender: Player) {
        if (BuildMode.toggle(sender)) {
            sender.sendRichMessage("<green>Modo de construção <white>ligado<green>.")
            return
        }

        sender.sendRichMessage("<red>Modo de construção <white>desligado<red>.")
    }
}
