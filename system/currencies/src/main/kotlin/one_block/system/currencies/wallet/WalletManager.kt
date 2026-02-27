package one_block.system.currencies.wallet

import one_block.libs.instance.Instance
import one_block.system.currencies.currency.Currency
import java.util.*

data class WalletManager(
    val currency: Currency,
) {

    private val walletsUuid = mutableMapOf<UUID, Wallet>()
    private val walletsName = mutableMapOf<String, UUID>()
    private val dirtyWallets = mutableSetOf<UUID>()

    private val repository = WalletRepository(currency)

    fun load(uuid: UUID, name: String) {
        val wallet = repository.find(uuid) ?: Wallet(uuid, name.lowercase(), currency.initialValue)
        register(wallet)
    }

    fun save(uuid: UUID) {
        find(uuid)?.let { repository.update(it) }
    }

    fun register(wallet: Wallet) {
        walletsUuid[wallet.uuid] = wallet
        walletsName[wallet.name.lowercase()] = wallet.uuid
    }

    fun unregister(wallet: Wallet) {
        walletsUuid.remove(wallet.uuid)
        walletsName.remove(wallet.name.lowercase())
    }

    fun find(uuid: UUID): Wallet? {
        return walletsUuid[uuid]
    }

    fun find(name: String): Wallet? {
        val uuid = walletsName[name.lowercase()] ?: return null
        return find(uuid)
    }

    fun markDirty(uuid: UUID) {
        dirtyWallets.add(uuid)
    }

    fun saveDirtyIslands() {
        if (dirtyWallets.isEmpty()) return

        val toSave = dirtyWallets.mapNotNull { walletsUuid[it] }.toList()
        repository.updateAll(toSave)

        dirtyWallets.clear()
        Instance.logger.info("[OneBlock] Salvas ${toSave.size} carteiras que sofreram alterações.")
    }

    fun topWallets() = repository.topWallets()
}