package one_block.system.spawners.repo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import one_block.system.spawners.models.PlacedSpawner
import java.util.UUID

class PlacedSpawnerRepository {

    private val collection: MongoCollection<PlacedSpawner> = Mongo.getCollection("spawners")

    private val upsertOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlacedSpawner> = collection.find().toList()

    fun update(placed: PlacedSpawner) {
        collection.replaceOne(Filters.eq("uuid", placed.uuid), placed, upsertOptions)
    }

    fun updateAll(list: List<PlacedSpawner>) {
        if (list.isEmpty()) return

        val replaceOptions = ReplaceOptions().upsert(false)
        val models = list.map { ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions) }
        collection.bulkWrite(models)
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }

}
