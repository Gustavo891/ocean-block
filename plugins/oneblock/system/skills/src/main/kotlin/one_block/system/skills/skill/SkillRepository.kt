package one_block.system.skills.skill

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import java.util.UUID

class SkillRepository {

    private val collection: MongoCollection<SkillData> = Mongo.getCollection("skills")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun find(uuid: UUID): SkillData? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun update(data: SkillData) {
        collection.replaceOne(Filters.eq("uuid", data.uuid), data, replaceOptions)
    }

    fun updateAll(list: List<SkillData>) {
        if (list.isEmpty()) return

        val models = list.map {
            ReplaceOneModel(Filters.eq("uuid", it.uuid), it, replaceOptions)
        }

        collection.bulkWrite(models)
    }

    fun remove(uuid: UUID) {
        collection.deleteOne(Filters.eq("uuid", uuid))
    }

    fun toList(): List<SkillData> = collection.find().toList()
}
