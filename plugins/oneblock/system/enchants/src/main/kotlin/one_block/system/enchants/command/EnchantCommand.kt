package one_block.system.enchants.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import one_block.system.enchants.Enchants
import one_block.system.enchants.book.EnchantBooks
import one_block.system.enchants.enchant.EnchantRegistry
import org.bukkit.entity.Player

@CommandAlias("encantamentos|customenchants")
class EnchantCommand : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        sender.sendRichMessage("<green>Encantamentos disponíveis:")

        EnchantRegistry.all().forEach { enchant ->
            sender.sendRichMessage(
                "<dark_gray>▸ <white>${enchant.id} <gray>- ${enchant.displayName(enchant.maxLevel)} " +
                    "<dark_gray>(${enchant.rarity.formatted()}<dark_gray>)"
            )
        }
    }

    @Subcommand("book")
    @CommandPermission("screen.dev")
    fun onBook(sender: Player, onlineTarget: OnlinePlayer, enchantId: String, level: Int) {
        val enchant = EnchantRegistry.find(enchantId)
        if (enchant == null) {
            sender.sendRichMessage("<red>Encantamento inválido. Use /encantamentos para listar.")
            return
        }

        val target = onlineTarget.player
        target.inventory.addItem(EnchantBooks.create(enchant, level))

        sender.sendRichMessage("<green>Livro de ${enchant.prettyName} $level entregue para ${target.name}.")
    }

    @Subcommand("apply")
    @CommandPermission("screen.dev")
    fun onApply(sender: Player, enchantId: String, level: Int) {
        val enchant = EnchantRegistry.find(enchantId)
        if (enchant == null) {
            sender.sendRichMessage("<red>Encantamento inválido. Use /encantamentos para listar.")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (item.type.isAir) {
            sender.sendRichMessage("<red>Segure um item na mão principal.")
            return
        }

        if (!Enchants.apply(item, enchant, level)) {
            sender.sendRichMessage("<red>Não foi possível aplicar o encantamento neste item.")
            return
        }

        sender.sendRichMessage("<green>${enchant.prettyName} $level aplicado.")
    }

    @Subcommand("remove")
    @CommandPermission("screen.dev")
    fun onRemove(sender: Player, enchantId: String) {
        val enchant = EnchantRegistry.find(enchantId)
        if (enchant == null) {
            sender.sendRichMessage("<red>Encantamento inválido. Use /encantamentos para listar.")
            return
        }

        val item = sender.inventory.itemInMainHand
        if (item.type.isAir) {
            sender.sendRichMessage("<red>Segure um item na mão principal.")
            return
        }

        Enchants.remove(item, enchant)
        sender.sendRichMessage("<green>${enchant.prettyName} removido.")
    }
}
