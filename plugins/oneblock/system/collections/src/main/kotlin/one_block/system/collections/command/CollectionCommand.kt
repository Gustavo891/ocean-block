package one_block.system.collections.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.collections.Collections
import one_block.system.collections.collection.CollectionEntry
import one_block.system.collections.view.CollectionsView
import org.bukkit.entity.Player

@CommandAlias("collections|colecoes|coleções")
class CollectionCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(CollectionsView::class.java, sender)
    }

    @Subcommand("add")
    @CommandPermission("screen.dev")
    fun onAdd(sender: Player, onlineTarget: OnlinePlayer, entryId: String, amount: Long) {
        val entry = CollectionEntry.byId(entryId)
        if (entry == null) {
            sender.sendRichMessage("<red>Coleção inválida. Use: ${CollectionEntry.entries.joinToString { it.id }}")
            return
        }

        val target = onlineTarget.player
        if (Collections.data(target.uniqueId) == null) {
            sender.sendRichMessage("<red>Dados de coleção do jogador não carregados.")
            return
        }

        Collections.add(target.uniqueId, entry, amount)
        sender.sendRichMessage("<green>Adicionado $amount em ${entry.prettyName} para ${target.name}.")
    }

    @Subcommand("set")
    @CommandPermission("screen.dev")
    fun onSet(sender: Player, onlineTarget: OnlinePlayer, entryId: String, amount: Long) {
        val entry = CollectionEntry.byId(entryId)
        if (entry == null) {
            sender.sendRichMessage("<red>Coleção inválida. Use: ${CollectionEntry.entries.joinToString { it.id }}")
            return
        }

        val target = onlineTarget.player
        if (Collections.data(target.uniqueId) == null) {
            sender.sendRichMessage("<red>Dados de coleção do jogador não carregados.")
            return
        }

        Collections.set(target.uniqueId, entry, amount)
        sender.sendRichMessage("<green>Coleção ${entry.prettyName} de ${target.name} definida em $amount.")
    }
}
