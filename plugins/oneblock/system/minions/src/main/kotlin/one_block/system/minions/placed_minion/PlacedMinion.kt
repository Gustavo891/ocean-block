package one_block.system.minions.placed_minion

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.libs.item_builder.ItemSerializer
import one_block.system.minions.enums.FilterMode
import one_block.system.minions.enums.FullMode
import one_block.system.minions.enums.MinionType
import one_block.system.minions.enums.MinionUpgrade
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class PlacedMinion(
    val uuid: UUID,
    val location: SimpleLocation,
    val type: MinionType,
    var energy: Int,
    val upgrades: MutableMap<String, Int> = mutableMapOf(),
    var enabled: Boolean = true,
    val storage: MutableList<String> = mutableListOf(),
    var fullMode: FullMode = FullMode.STOP,
    var filterMode: FilterMode = FilterMode.BLACKLIST,
    val filter: MutableSet<String> = mutableSetOf(),
    var facing: String? = null,
    var xp: Int? = null,
    var owner: String? = null,
    var produced: Int? = null,
    var energySpent: Int? = null,
    var placedAt: Long? = null,
) {

    companion object {
        const val MAX_ENERGY = 3_000

        const val STORAGE_BASE_SLOTS = 9
    }


    private var undecodedStorage: List<String> = emptyList()

    private val cachedItemsLazy: Lazy<MutableList<ItemStack>> = lazy {
        val failed = mutableListOf<String>()
        val decoded = storage.mapNotNullTo(mutableListOf()) { encoded ->
            val item = runCatching { ItemSerializer.deserialize(encoded) }.getOrNull()
            if (item == null) failed.add(encoded)
            item
        }
        undecodedStorage = failed

        if (decoded.size < storage.size) {
            Instance.logger.warning(
                "[minions] minion $uuid ($type): ${storage.size - decoded.size} de ${storage.size} " +
                    "entrada(s) de storage não puderam ser decodificadas; ficam fora do minion, " +
                        "mas o dado original continua salvo."
            )
        }

        decoded
    }

    private val cachedItems: MutableList<ItemStack> get() = cachedItemsLazy.value

    fun hasEnergy() = energy > 0

    fun addEnergy(amount: Int) {
        energy = (energy + amount).coerceAtMost(getMaxEnergy())
    }

    fun consumeEnergy(amount: Int) {
        val spent = minOf(amount, energy)
        energy = (energy - amount).coerceAtLeast(0)
        if (spent > 0) energySpent = energySpentTotal + spent
    }

    val ownerUuid: UUID?
        get() = owner?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    val producedCount: Int get() = produced ?: 0

    val energySpentTotal: Int get() = energySpent ?: 0

    fun countProduced(amount: Int = 1) {
        if (amount <= 0) return
        produced = producedCount + amount
    }

    fun getMaxEnergy(): Int {
        return MAX_ENERGY * getUpgradeLevel(MinionUpgrade.Battery, 1)
    }

    fun setUpgradeLevel(id: String, level: Int) {
        upgrades[id] = level
    }

    fun getUpgradeLevel(id: String, def: Int = 0): Int {
        return upgrades.getOrDefault(id, def)
    }

    fun setUpgradeLevel(upgrade: MinionUpgrade, level: Int) {
        upgrades[upgrade.id] = level
    }

    fun getUpgradeLevel(upgrade: MinionUpgrade, def: Int = 0): Int {
        return upgrades.getOrDefault(upgrade.id, def)
    }

    fun accepts(item: ItemStack): Boolean {
        if (filter.isEmpty()) return true
        val listed = filter.any { MinionFilter.matches(it, item) }
        return when (filterMode) {
            FilterMode.WHITELIST -> listed
            FilterMode.BLACKLIST -> !listed
        }
    }

    fun storageCapacity(): Int =
        MinionUpgrade.storageSlotsFor(getUpgradeLevel(MinionUpgrade.MinerStorage))


    fun storageItems(): List<ItemStack> = cachedItems.map { it.clone() }

    fun setStorage(items: List<ItemStack>) {
        cachedItems.clear()
        cachedItems.addAll(items.map { it.clone() })
    }

    fun isStorageFull(): Boolean = cachedItems.size >= storageCapacity()

    fun isPaused(): Boolean = fullMode == FullMode.STOP && isStorageFull()

    fun addToStorage(items: List<ItemStack>): List<ItemStack> {
        val capacity = storageCapacity()
        val leftover = mutableListOf<ItemStack>()

        for (incoming in items) {
            val remaining = incoming.clone()

            for (slot in cachedItems) {
                if (remaining.amount <= 0) break
                if (!slot.isSimilar(remaining)) continue

                val space = slot.maxStackSize - slot.amount
                if (space <= 0) continue

                val moved = minOf(space, remaining.amount)
                slot.amount += moved
                remaining.amount -= moved
            }

            while (remaining.amount > 0 && cachedItems.size < capacity) {
                val taken = minOf(remaining.maxStackSize, remaining.amount)
                cachedItems.add(remaining.clone().apply { amount = taken })
                remaining.amount -= taken
            }

            if (remaining.amount > 0) leftover += remaining
        }

        return leftover
    }

    val xpAmount: Int get() = xp ?: 0

    fun addXp(amount: Int) {
        if (amount <= 0) return
        xp = xpAmount + amount
    }

    fun takeXp(): Int {
        val current = xpAmount
        xp = null
        return current
    }

    fun sync() {
        if (cachedItemsLazy.isInitialized()) {
            storage.clear()
            cachedItems.forEach { storage.add(ItemSerializer.serialize(it)) }
            storage.addAll(undecodedStorage)
        }
    }
}
