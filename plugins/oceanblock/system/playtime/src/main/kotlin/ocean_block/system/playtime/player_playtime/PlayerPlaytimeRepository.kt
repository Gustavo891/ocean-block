package ocean_block.system.playtime.player_playtime

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import java.util.*

class PlayerPlaytimeRepository {

    private val collection: MongoCollection<PlayerPlaytime> = Mongo.getCollection("playtimes")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun find(uuid: UUID): PlayerPlaytime? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun update(playtime: PlayerPlaytime) {
        val filter = Filters.eq("uuid", playtime.uuid)
        collection.replaceOne(filter, playtime, replaceOptions)
    }

    fun updateAll(list: List<PlayerPlaytime>) {
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

    fun topPlaytimeList() = collection.find().sort(Sorts.descending("playtime"))
        .limit(10)
        .toList()
}
