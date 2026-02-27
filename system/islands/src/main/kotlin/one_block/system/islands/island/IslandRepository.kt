package one_block.system.islands.island

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.*

class IslandRepository {

    private val collection: MongoCollection<Island> = Mongo.getCollection("islands")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun update(island: Island) {
        val filter = Filters.eq("uuid", island.uuid)
        collection.replaceOne(filter, island, replaceOptions)
    }

    fun updateAll(list: List<Island>) {
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

    fun toList(): List<Island> {
        return collection.find().toList()
    }
}
