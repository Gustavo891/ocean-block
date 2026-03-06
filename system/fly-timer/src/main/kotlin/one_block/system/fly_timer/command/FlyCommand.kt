package one_block.system.fly_timer.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import one_block.system.fly_timer.Flys
import one_block.system.fly_timer.fly_player.FlyPlayerManager
import org.bukkit.entity.Player

@CommandAlias("voar")
class FlyCommand: BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        val flyPlayer = FlyPlayerManager.get(sender.uniqueId)

        if (flyPlayer == null) {
            sender.sendRichMessage("<red>Dados de fly não carregados.</red>")
            return
        }

        if (!flyPlayer.hasTime()) {
            sender.sendRichMessage("<red>Você não possui tempo de voo.</red>")
            return
        }

        if (sender.allowFlight) {

            sender.allowFlight = false
            sender.isFlying = false
            flyPlayer.stopFlying()

            sender.sendRichMessage("<yellow>Fly desativado.</yellow>")
            return
        }

        sender.allowFlight = true
        flyPlayer.startFlying()

        sender.sendRichMessage("<green>Fly ativado.</green>")
    }

    @Subcommand("add")
    @CommandPermission("screen.dev")
    fun onAdd(sender: Player, onlineTarget: OnlinePlayer) {
        val target = onlineTarget.player
        val flyPlayer = FlyPlayerManager.get(target.uniqueId)

        if (flyPlayer == null) {
            sender.sendRichMessage("<red>Jogador não carregado.</red>")
            return
        }

        flyPlayer.addSeconds(60)

        sender.sendRichMessage("<green>Adicionou 60 segundos para ${target.name}</green>")
        target.sendRichMessage("<green>Você recebeu 60 segundos de fly.</green>")
    }

    @Subcommand("item")
    @CommandPermission("screen.dev")
    fun onITem(sender: Player, onlineTarget: OnlinePlayer) {
        val target = onlineTarget.player
        target.inventory.addItem(Flys.createFlyItem())

        sender.sendRichMessage("<green>Deu o item ggggggggggggg")
    }
}