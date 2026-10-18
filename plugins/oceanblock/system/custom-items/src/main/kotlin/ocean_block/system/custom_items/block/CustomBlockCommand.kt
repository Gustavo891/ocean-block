package ocean_block.system.custom_items.block

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("customblock|cblock")
@CommandPermission("screen.dev")
class CustomBlockCommand : BaseCommand() {

    @Subcommand("give")
    @CommandCompletion("@customblocks @players @range:1-64")
    fun give(
        sender: Player,
        id: String,
        @Optional target: OnlinePlayer?,
        @Default("1") @Optional amount: Int,
    ) {
        val block = CustomBlockRegistry.get(id)
        if (block == null) {
            sender.sendRichMessage("<red>Bloco desconhecido: <white>$id")
            return
        }

        val receiver = target?.player ?: sender
        val amt = amount.coerceIn(1, 64)
        val overflow = receiver.inventory.addItem(block.buildItem(amount = amt))

        overflow.values.forEach { receiver.world.dropItemNaturally(receiver.location, it) }
        sender.sendRichMessage("<green>Entregue <white>${amt}x $id<green> para <white>${receiver.name}")
    }

    @Subcommand("list")
    fun list(sender: CommandSender) {
        val ids = CustomBlockRegistry.ids()
        if (ids.isEmpty()) {
            sender.sendRichMessage("<gray>Nenhum bloco custom registrado.")
            return
        }
        sender.sendRichMessage("<yellow>Blocos custom (${ids.size}): <white>${ids.sorted().joinToString(", ")}")
    }
}
