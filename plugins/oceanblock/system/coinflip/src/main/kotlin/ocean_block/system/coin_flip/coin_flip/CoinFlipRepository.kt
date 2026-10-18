package ocean_block.system.coin_flip.coin_flip

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo

class CoinFlipRepository {

    private val collection: MongoCollection<CoinFlip> = Mongo.getCollection("coinflip")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<CoinFlip> {
        return collection.find().toList()
    }

    fun updateAll(list: List<CoinFlip>) {
        if (list.isEmpty()) return

        val models = list.map {
            val filter = Filters.eq("uuid", it.uuid)
            ReplaceOneModel(filter, it, replaceOptions)
        }

        collection.bulkWrite(models)
    }

}
