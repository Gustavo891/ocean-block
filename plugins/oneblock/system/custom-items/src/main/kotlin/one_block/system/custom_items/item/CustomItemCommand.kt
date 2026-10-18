package one_block.system.custom_items.item

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

@CommandAlias("customitem|citem")
@CommandPermission("screen.dev")
class CustomItemCommand : BaseCommand() {

    @Subcommand("give")
    @CommandCompletion("@customitems @players @range:1-64")
    fun give(
        sender: Player,
        id: String,
        @Optional target: OnlinePlayer?,
        @Default("1") @Optional amount: Int,
    ) {
        val item = CustomItemRegistry.get(id)
        if (item == null) {
            sender.sendRichMessage("<red>Item desconhecido: <white>$id")
            return
        }

        val receiver = target?.player ?: sender
        val amt = amount.coerceIn(1, 64)
        val overflow = receiver.inventory.addItem(item.build(amt))

        overflow.values.forEach { receiver.world.dropItemNaturally(receiver.location, it) }
        sender.sendRichMessage("<green>Entregue <white>${amt}x $id<green> para <white>${receiver.name}")
    }

    @Subcommand("list")
    fun list(sender: CommandSender) {
        val ids = CustomItemRegistry.ids()
        if (ids.isEmpty()) {
            sender.sendRichMessage("<gray>Nenhum item custom registrado.")
            return
        }
        sender.sendRichMessage("<yellow>Itens custom (${ids.size}): <white>${ids.sorted().joinToString(", ")}")
    }
}
