package one_block.system.auction_house.auction

import one_block.system.auction_house.serializer.ItemSerializer
import java.util.*

data class Auction(
    val uuid: UUID,
    val sellerName: String,
    val price: Double,
    val itemBase64: String,
) {

    val item by lazy { ItemSerializer.deserialize(itemBase64) }
}