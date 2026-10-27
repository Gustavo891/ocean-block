package ocean_block.system.pirate_raid.data

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import ocean_block.libs.connection.Mongo
import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

data class PirateRaidDay(
    val islandUuid: String,
    val lastDay: String,
)

class PirateRaidRepository {

    private val collection: MongoCollection<PirateRaidDay> = Mongo.getCollection("pirate_raids")

    init {
        collection.createIndex(Indexes.ascending("islandUuid"), IndexOptions().unique(true))
    }

    fun toList(): List<PirateRaidDay> = collection.find().toList()

    fun save(day: PirateRaidDay) {
        collection.replaceOne(Filters.eq("islandUuid", day.islandUuid), day, ReplaceOptions().upsert(true))
    }
}

object RaidDays {

    private val ZONE = ZoneId.of("America/Sao_Paulo")

    private lateinit var repository: PirateRaidRepository
    private val days = ConcurrentHashMap<UUID, String>()

    fun onInit() {
        repository = PirateRaidRepository()
        repository.toList().forEach { day ->
            runCatching { UUID.fromString(day.islandUuid) }.getOrNull()?.let { days[it] = day.lastDay }
        }
    }

    fun today(): String = LocalDate.now(ZONE).toString()

    fun lastDay(island: UUID): String? = days[island]

    fun mark(island: UUID, day: String) {
        days[island] = day
        ScheduledExecutor.execute {
            runCatching { repository.save(PirateRaidDay(island.toString(), day)) }
                .onFailure { Instance.logger.log(Level.SEVERE, "[piratas] Falha ao salvar a raid do dia da ilha $island", it) }
        }
    }
}
