package one_block.system.auction_house.auction

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AuctionManager {

    val auctions = ConcurrentHashMap<UUID, Auction>()
    private val repository = AuctionRepository()

    fun onInit() {
        repository.toList().forEach { auctions[it.uuid] = it }
    }

    fun register(auction: Auction) {
        auctions[auction.uuid] = auction
        repository.update(auction)
    }
}