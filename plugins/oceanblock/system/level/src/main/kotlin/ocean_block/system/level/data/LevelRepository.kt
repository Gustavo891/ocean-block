package ocean_block.system.level.data

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo

class LevelRepository {

    private val collection: MongoCollection<PlayerLevel> = Mongo.getCollection("player_levels")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlayerLevel> = collection.find().toList()

    fun updateAll(list: List<PlayerLevel>) {
        if (list.isEmpty()) return
        collection.bulkWrite(list.map { ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions) })
    }
}
