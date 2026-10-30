package ocean_block.system.islands.account

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import java.util.UUID

class PendingResetRepository {

    private val collection: MongoCollection<PendingAccountReset> = Mongo.getCollection("account_resets")

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun toList(): List<UUID> = collection.find().toList().map { it.uuid }

    fun add(uuid: UUID) {
        collection.replaceOne(Filters.eq("uuid", uuid), PendingAccountReset(uuid), ReplaceOptions().upsert(true))
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }
}
