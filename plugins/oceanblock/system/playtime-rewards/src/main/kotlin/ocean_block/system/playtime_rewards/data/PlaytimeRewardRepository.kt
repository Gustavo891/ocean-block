package ocean_block.system.playtime_rewards.data

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo

class PlaytimeRewardRepository {

    private val collection: MongoCollection<PlaytimeRewardData> = Mongo.getCollection("playtime_rewards")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlaytimeRewardData> = collection.find().toList()

    fun updateAll(list: List<PlaytimeRewardData>) {
        if (list.isEmpty()) return
        collection.bulkWrite(list.map { ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions) })
    }
}
