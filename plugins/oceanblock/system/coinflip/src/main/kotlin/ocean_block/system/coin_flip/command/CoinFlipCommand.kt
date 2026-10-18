package ocean_block.system.coin_flip.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import de.rapha149.signgui.SignGUI
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.coin_flip.coin_flip.CoinFlipManager
import ocean_block.system.coin_flip.view.CoinFlipView
import ocean_block.system.currencies.Currencies
import org.bukkit.entity.Player

@CommandAlias("coinflip|cf")
class CoinFlipCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(CoinFlipView::class.java, sender)
    }

    @Subcommand("criar")
    fun onCreate(sender: Player) {
        SignGUI.builder()
            .setLines(
                "",
                "^^^^^^^^",
                "Digite o valor",
                "Ex: 1000"
            )
            .setHandler { player, result ->
                val input = result.getLine(0)

                val amount = input.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    player.sendRichMessage("<red>Valor inválido.")
                    return@setHandler emptyList()
                }

                if (!Currencies.has(player.uniqueId, "coins", amount)) {
                    player.sendRichMessage("<red>Sem dinheiro!.")
                    return@setHandler emptyList()
                }

                Currencies.withdraw(player.uniqueId, "coins", amount)
                val coinFlip = CoinFlipManager.createGame(
                    player,
                    amount
                )

                player.sendRichMessage("<green>CoinFlip criado com valor de $amount!")

                emptyList()
            }
            .build()
            .open(sender)
    }
}
