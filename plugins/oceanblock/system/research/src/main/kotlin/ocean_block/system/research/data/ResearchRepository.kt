package ocean_block.system.research.data

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import ocean_block.system.research.model.IslandResearch

class ResearchRepository {

    private val collection: MongoCollection<IslandResearch> = Mongo.getCollection("island_research")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("islandUuid"), IndexOptions().unique(true))
    }

    fun toList(): List<IslandResearch> = collection.find().toList()

    fun update(state: IslandResearch) {
        collection.replaceOne(Filters.eq("islandUuid", state.islandUuid), state, replaceOptions)
    }

    fun updateAll(list: List<IslandResearch>) {
        if (list.isEmpty()) return
        collection.bulkWrite(list.map { ReplaceOneModel(Filters.eq("islandUuid", it.islandUuid), it, replaceOptions) })
    }
}
