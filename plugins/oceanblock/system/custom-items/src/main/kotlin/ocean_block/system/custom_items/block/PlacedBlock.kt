package ocean_block.system.custom_items.block

import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.libs.instance.Instance
import ocean_block.libs.item_builder.ItemSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID


data class PlacedBlock(
    val uuid: UUID,
    val location: SimpleLocation,
    val blockId: String,
    val ownerUuid: UUID,
    var enabled: Boolean = true,
    val upgrades: MutableMap<String, Int> = mutableMapOf(),
    val storage: MutableList<String> = mutableListOf(),
    var filterMode: FilterMode = FilterMode.BLACKLIST,
    val filter: MutableSet<String> = mutableSetOf(),
    var notifiedFull: Boolean = false,
) {

    fun getUpgradeLevel(id: String, def: Int = 0): Int = upgrades.getOrDefault(id, def)

    fun getUpgradeLevel(upgrade: BlockUpgrade, def: Int = 0): Int = upgrades.getOrDefault(upgrade.id, def)

    fun setUpgradeLevel(upgrade: BlockUpgrade, level: Int) {
        upgrades[upgrade.id] = level
    }

    fun accepts(material: Material): Boolean = FilterRules.accepts(filterMode, filter, material.name)

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
                "[custom-items] bloco $uuid ($blockId): ${storage.size - decoded.size} de ${storage.size} " +
                    "entrada(s) de storage não puderam ser decodificadas; ficam fora do bloco, " +
                        "mas o dado original continua salvo."
            )
        }

        decoded
    }

    private val cachedItems: MutableList<ItemStack> get() = cachedItemsLazy.value

    fun storageItems(): List<ItemStack> = cachedItems.map { it.clone() }

    fun setStorage(items: List<ItemStack>) {
        cachedItems.clear()
        cachedItems.addAll(items.map { it.clone() })
    }

    fun setStorage(inventory: Inventory) {
        setStorage(inventory.contents.filterNotNull())
    }

    fun syncStorage() {
        if (!cachedItemsLazy.isInitialized()) return
        storage.clear()
        cachedItems.forEach { storage.add(ItemSerializer.serialize(it)) }
        storage.addAll(undecodedStorage)
    }

    fun isStorageFull(capacity: Int): Boolean {
        val inventory = buildStorageInventory(capacity)
        return inventory.firstEmpty() == -1 &&
            inventory.contents.filterNotNull().all { it.amount >= it.maxStackSize }
    }

    fun buildStorageInventory(capacity: Int): Inventory {
        val inventory = Bukkit.createInventory(null, capacity)
        val leftover = inventory.addItem(*storageItems().toTypedArray())

        if (leftover.isNotEmpty()) {
            Instance.logger.warning(
                "[custom-items] bloco $uuid ($blockId): ${leftover.size} stack(s) não couberam " +
                    "na capacidade de $capacity slots e serão descartados."
            )
        }

        return inventory
    }
}
