package one_block.system.fly_timer.fly_player

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.*

class FlyPlayerRepository {

    private val collection: MongoCollection<FlyPlayer> = Mongo.getCollection("fly_players")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun find(uuid: UUID): FlyPlayer? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun update(flyPlayer: FlyPlayer) {
        val filter = Filters.eq("uuid", flyPlayer.uuid)
        collection.replaceOne(filter, flyPlayer, replaceOptions)
    }

    fun updateAll(list: List<FlyPlayer>) {
        if (list.isEmpty()) return

        val models = list.map {
            val filter = Filters.eq("uuid", it.uuid)
            ReplaceOneModel(filter, it, replaceOptions)
        }

        collection.bulkWrite(models)
    }
}
