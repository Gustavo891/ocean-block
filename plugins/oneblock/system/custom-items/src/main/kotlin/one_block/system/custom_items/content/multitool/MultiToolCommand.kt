package one_block.system.custom_items.content.multitool

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import one_block.system.custom_items.item.CustomItemRegistry
import org.bukkit.entity.Player

@CommandAlias("multitool")
@CommandPermission("oneblock.multitool")
class MultiToolCommand : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        val item = CustomItemRegistry.get("multitool")
        if (item == null) {
            player.sendRichMessage("<red>MultiTool nao esta registrada.")
            return
        }

        player.inventory.addItem(item.build())
        player.sendRichMessage("<green>Você recebeu a <gradient:#f7971e:#ffd200>MultiTool</gradient><green>!")
    }
}
