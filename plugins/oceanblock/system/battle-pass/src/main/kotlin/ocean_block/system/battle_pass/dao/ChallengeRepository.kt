package ocean_block.system.battle_pass.dao

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import ocean_block.system.battle_pass.challenge.PlayerChallengeData
import java.util.UUID

class ChallengeRepository {

    private val collection: MongoCollection<PlayerChallengeData> = Mongo.getCollection("battle_pass_challenges")
    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("playerUuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlayerChallengeData> = collection.find().toList()

    fun update(data: PlayerChallengeData) {
        collection.replaceOne(Filters.eq("playerUuid", data.playerUuid), data, replaceOptions)
    }

    fun find(playerUuid: UUID): PlayerChallengeData? {
        return collection.find(Filters.eq("playerUuid", playerUuid)).firstOrNull()
    }
}
