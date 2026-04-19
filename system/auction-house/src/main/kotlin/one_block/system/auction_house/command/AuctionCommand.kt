package one_block.system.auction_house.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.formatter.ext.format
import one_block.system.auction_house.auction.Auction
import one_block.system.auction_house.auction.AuctionManager
import one_block.libs.item_builder.ItemSerializer
import one_block.system.auction_house.view.AuctionView
import org.bukkit.entity.Player
import java.util.*

@CommandAlias("mercado|ah")
class AuctionCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        viewFrame.open(AuctionView::class.java, sender)
    }

    @Subcommand("vender")
    fun onDefault(sender: Player, price: Double) {
        val itemInMainHand = sender.inventory.itemInMainHand
        if (itemInMainHand.isEmpty) {
            sender.sendRichMessage("<red>Você não pode vender sua mão.")
            return
        }

        val auction = Auction(
            UUID.randomUUID(),
            sender.uniqueId,
            sender.name.lowercase(),
            price,
            ItemSerializer.serialize(itemInMainHand)
        )

        AuctionManager.register(auction)
        itemInMainHand.amount = 0

        sender.sendRichMessage("<green>Você colocou para vender por <green>$<white>${price.format}<green>.")
    }

    @Subcommand("list")
    @CommandPermission("screen.dev")
    fun onList(sender: Player) {
        AuctionManager.auctions.values.forEach { auction -> sender.sendRichMessage("${auction}") }
    }
}