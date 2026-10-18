package one_block.system.xpbottle.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import one_block.system.xpbottle.XpBottleItem
import org.bukkit.entity.Player

@CommandAlias("xpbottle|xpb")
class XpBottleCommand : BaseCommand() {

    private val MIN_LEVELS = 5

    @Default
    fun onDefault(sender: Player, levels: Int) {
        if (levels < MIN_LEVELS) {
            sender.sendRichMessage("<red>Mínimo de $MIN_LEVELS níveis.")
            return
        }

        if (sender.level < levels) {
            sender.sendRichMessage("<red>Você não tem níveis suficientes.")
            return
        }

        sender.level -= levels
        val bottleItem = XpBottleItem.create(levels)

        sender.inventory.addItem(bottleItem)
        sender.sendRichMessage("<green>Você guardou $levels níveis em um pote.")
    }
}
