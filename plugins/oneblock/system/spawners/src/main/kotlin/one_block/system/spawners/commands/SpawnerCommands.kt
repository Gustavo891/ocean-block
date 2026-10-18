package one_block.system.spawners.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.spawners.models.SpawnerType
import one_block.system.spawners.SpawnersSystem
import one_block.system.spawners.views.SpawnerShopView
import org.bukkit.entity.Player

@CommandAlias("spawner|spawners")
class SpawnerCommands(private val viewFrame: ViewFrame) : BaseCommand() {

    @Subcommand("loja|shop")
    fun shop(sender: Player) {
        viewFrame.open(SpawnerShopView::class.java, sender)
    }

    @Subcommand("give")
    @CommandPermission("screen.dev")
    @CommandCompletion("@spawnertypes @players @range:1-64")
    fun give(
        sender: Player,
        id: String,
        @Optional target: OnlinePlayer?,
        @Default("1") @Optional amount: Int,
    ) {
        val type = SpawnerType.find(id.uppercase())
        if (type == null) {
            sender.sendRichMessage("<red>Spawner desconhecido: <white>$id")
            return
        }

        val receiver = target?.player ?: sender
        val amt = amount.coerceIn(1, 64)
        val overflow = receiver.inventory.addItem(SpawnersSystem.build(type, amt))

        overflow.values.forEach { receiver.world.dropItemNaturally(receiver.location, it) }
        sender.sendRichMessage("<green>Entregue <white>${amt}x ${type.name}<green> para <white>${receiver.name}")
    }

    @Subcommand("list")
    @CommandPermission("screen.dev")
    fun list(sender: Player) {
        val ids = SpawnerType.entries.joinToString(", ") { it.name }
        sender.sendRichMessage("<yellow>Spawners (${SpawnerType.entries.size}): <white>$ids")
    }
}
