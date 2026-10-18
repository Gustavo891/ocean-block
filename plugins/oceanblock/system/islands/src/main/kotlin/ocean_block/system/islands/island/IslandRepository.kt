package ocean_block.system.islands.island

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import ocean_block.libs.instance.Instance
import java.util.*

class IslandRepository {

    private val collection: MongoCollection<Island> = Mongo.getCollection("islands")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun migrateMissingFields() {
        val defaults = listOf(
            "trustedNames" to emptyList<String>(),
            "members" to emptyMap<String, Any>(),
            "upgrades" to emptyMap<String, Any>(),
            "pendingRewardPhases" to emptyList<String>(),
            "currentPhaseId" to "planicie",
            "grid" to 0,
            "size" to 30,
            "isPublic" to false,
        )

        var repaired = 0L

        defaults.forEach { (field, value) ->
            val filter = Filters.or(Filters.exists(field, false), Filters.eq(field, null))
            repaired += collection.updateMany(filter, Updates.set(field, value)).modifiedCount
        }

        if (repaired > 0L) {
            Instance.logger.info("[islands] $repaired campo(s) ausente(s) preenchido(s) em ilhas antigas.")
        }
    }

    fun update(island: Island) {
        val filter = Filters.eq("uuid", island.uuid)
        collection.replaceOne(filter, island, replaceOptions)
    }

    fun updateAll(list: List<Island>) {
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

    fun toList(): List<Island> {
        return collection.find().toList()
    }
}
