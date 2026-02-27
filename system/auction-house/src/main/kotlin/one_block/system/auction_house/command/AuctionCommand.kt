package one_block.system.auction_house.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.auction_house.auction.Auction
import one_block.system.auction_house.auction.AuctionManager
import one_block.system.auction_house.serializer.ItemSerializer
import one_block.system.auction_house.view.AuctionView
import org.bukkit.entity.Player
import java.util.*

@CommandAlias("mercado")
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
            sender.sendRichMessage("hoho")
            return
        }

        val auction = Auction(
            UUID.randomUUID(),
            sender.name.lowercase(),
            price,
            ItemSerializer.serialize(itemInMainHand)
        )

        AuctionManager.register(auction)
        sender.sendRichMessage("<green>Você colocou para vender x por <green>$<white>$price<green>.")
    }

    @Subcommand("list")
    fun onList(sender: Player) {
        AuctionManager.auctions.values.forEach { auction -> sender.sendRichMessage("${auction}") }
    }
}