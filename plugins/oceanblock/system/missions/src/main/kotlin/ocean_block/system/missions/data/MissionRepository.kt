package ocean_block.system.missions.data

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import ocean_block.system.missions.model.PlayerMissions

class MissionRepository {

    private val collection: MongoCollection<PlayerMissions> = Mongo.getCollection("player_missions")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlayerMissions> = collection.find().toList()

    fun updateAll(list: List<PlayerMissions>) {
        if (list.isEmpty()) return
        collection.bulkWrite(list.map { ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions) })
    }
}
