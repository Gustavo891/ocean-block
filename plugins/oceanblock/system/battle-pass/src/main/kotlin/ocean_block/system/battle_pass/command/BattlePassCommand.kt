package ocean_block.system.battle_pass.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.battle_pass.player.BattlePassManager
import ocean_block.system.battle_pass.util.MessageUtil
import ocean_block.system.battle_pass.view.BattlePassView
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("passe|battlepass|bp")
class BattlePassCommand(private val viewFrame: ViewFrame) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(BattlePassView::class.java, sender, 0)
    }

    @Subcommand("addxp")
    @CommandPermission("screen.dev")
    fun onAddXp(sender: CommandSender, target: String, amount: Int) {
        val player = Bukkit.getPlayerExact(target)
        if (player == null) {
            sender.sendRichMessage(MessageUtil.PLAYER_NOT_FOUND)
            return
        }
        BattlePassManager.addXp(player, amount)
        sender.sendRichMessage(
            MessageUtil.XP_GRANTED,
            Placeholder.parsed("amount", amount.toString()),
            Placeholder.parsed("name", player.name),
        )
    }
}
