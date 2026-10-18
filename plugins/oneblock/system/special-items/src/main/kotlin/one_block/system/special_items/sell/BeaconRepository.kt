package one_block.system.special_items.sell

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.UUID

class BeaconRepository {

    private val collection: MongoCollection<SellBeacon> = Mongo.getCollection("sellbeacons")
    private val replaceOption: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun save(sellBeacon: SellBeacon) {
        collection.replaceOne(
            Filters.eq("uuid", sellBeacon.uuid),
            sellBeacon,
            replaceOption
        )
    }

    fun saveAll(sellBeacons: List<SellBeacon>) {
        collection.bulkWrite(sellBeacons.map {
            val filter = Filters.eq("uuid", it.uuid)
            ReplaceOneModel(filter, it, replaceOption)
        })
    }

    fun find(uuid: String): SellBeacon? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }

    fun toList(): List<SellBeacon> {
        return collection.find().toList()
    }
}
