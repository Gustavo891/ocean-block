package one_block.system.minions.placed_minion

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.*

class PlacedMinionRepository {

    private val collection: MongoCollection<PlacedMinion> = Mongo.getCollection("minions")
    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlacedMinion> {
        return collection.find().toList()
    }

    fun update(playtime: PlacedMinion) {
        val filter = Filters.eq("uuid", playtime.uuid)
        collection.replaceOne(filter, playtime, replaceOptions)
    }

    fun remove(uuid: UUID) {
        val filter = Filters.eq("uuid", uuid)
        collection.deleteOne(filter)
    }
}
