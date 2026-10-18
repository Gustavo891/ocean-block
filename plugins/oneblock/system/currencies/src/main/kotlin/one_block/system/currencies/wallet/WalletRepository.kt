package one_block.system.currencies.wallet

import com.mongodb.client.model.*
import com.mongodb.kotlin.client.MongoCollection
import one_block.libs.connection.Mongo
import one_block.system.currencies.currency.Currency
import java.util.*

class WalletRepository(
    currency: Currency,
) {

    private val collection: MongoCollection<Wallet> = Mongo.getCollection("wallet_${currency.id}")

    private val replaceOptions: ReplaceOptions = ReplaceOptions().upsert(true)

    init {
        collection.createIndex(Indexes.ascending("uuid"), IndexOptions().unique(true))
    }

    fun find(uuid: UUID): Wallet? {
        return collection.find(Filters.eq("uuid", uuid)).firstOrNull()
    }

    fun update(island: Wallet) {
        val filter = Filters.eq("uuid", island.uuid)
        collection.replaceOne(filter, island, replaceOptions)
    }

    fun updateAll(list: List<Wallet>) {
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

    fun toList(): List<Wallet> {
        return collection.find().toList()
    }

    fun topWallets() = collection.find().sort(Sorts.descending("balance"))
        .limit(10)
        .toList()
}
