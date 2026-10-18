package one_block.system.collections.collection

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.UUID

class CollectionRepository {

    private val collection: MongoCollection<CollectionData> = Mongo.getCollection("collections")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun find(uuid: UUID): CollectionData? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun update(data: CollectionData) {
        collection.replaceOne(Filters.eq("uuid", data.uuid), data, replaceOptions)
    }

    fun updateAll(list: List<CollectionData>) {
        if (list.isEmpty()) return

        val models = list.map {
            ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions)
        }

        collection.bulkWrite(models)
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }

    fun toList(): List<CollectionData> = collection.find().toList()
}
