package one_block.system.custom_items.block

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.UUID

class PlacedBlockRepository {

    private val collection: MongoCollection<PlacedBlock> = Mongo.getCollection("custom_blocks")

    private val upsertOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlacedBlock> = collection.find().toList()

    fun update(placed: PlacedBlock) {
        collection.replaceOne(Filters.eq("uuid", placed.uuid), placed, upsertOptions)
    }

    fun updateAll(list: List<PlacedBlock>) {
        if (list.isEmpty()) return

        val replaceOptions = ReplaceOptions().upsert(false)
        val models = list.map { ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions) }
        collection.bulkWrite(models)
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }
}
