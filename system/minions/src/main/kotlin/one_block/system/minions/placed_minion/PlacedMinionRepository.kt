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

    fun find(uuid: UUID): PlacedMinion? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun update(playtime: PlacedMinion) {
        val filter = Filters.eq("uuid", playtime.uuid)
        collection.replaceOne(filter, playtime, replaceOptions)
    }

    fun updateAll(list: List<PlacedMinion>) {
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
}