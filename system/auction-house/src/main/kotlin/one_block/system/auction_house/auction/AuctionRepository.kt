package one_block.system.auction_house.auction

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.*

class AuctionRepository {

    private val collection: MongoCollection<Auction> = Mongo.getCollection("auctions")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun find(uuid: UUID): Auction? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun update(auction: Auction) {
        val filter = Filters.eq("uuid", auction.uuid)
        collection.replaceOne(filter, auction, replaceOptions)
    }

    fun updateAll(list: List<Auction>) {
        if (list.isEmpty()) return

        val models = list.map {
            val filter = Filters.eq("uuid", it.uuid)
            ReplaceOneModel(filter, it, replaceOptions)
        }

        collection.bulkWrite(models)
    }

    fun remove(uuid: UUID) {
        val filter = Filters.eq("uuid", uuid)
        collection.deleteOne(filter)
    }

    fun toList(): List<Auction> {
        return collection.find().toList()
    }
}
