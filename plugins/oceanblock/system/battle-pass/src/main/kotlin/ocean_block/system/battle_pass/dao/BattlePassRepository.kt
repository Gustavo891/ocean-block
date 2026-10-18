package ocean_block.system.battle_pass.dao

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import ocean_block.system.battle_pass.player.PlayerPassData
import java.util.UUID

class BattlePassRepository {

    private val collection: MongoCollection<PlayerPassData> = Mongo.getCollection("battle_pass_players")
    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("playerUuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PlayerPassData> = collection.find().toList()

    fun update(playerBP: PlayerPassData) {
        collection.replaceOne(Filters.eq("playerUuid", playerBP.playerUuid), playerBP, replaceOptions)
    }

    fun find(uuid: UUID): PlayerPassData? {
        return collection.find(Filters.eq("playerUuid", uuid)).firstOrNull()
    }
}
