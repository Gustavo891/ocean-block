package one_block.system.custom_items.block

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.item_builder.nbt
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object PlacedBlockCodec {

    fun write(meta: ItemMeta, placed: PlacedBlock) {
        placed.syncStorage()

        meta.nbt {
            string(CustomBlockKeys.UUID, placed.uuid.toString())
            int(CustomBlockKeys.ENABLED, if (placed.enabled) 1 else 0)
            string(CustomBlockKeys.FILTER_MODE, placed.filterMode.name)

            val upgrades = StateCodec.encodeUpgrades(placed.upgrades)
            if (upgrades.isNotEmpty()) string(CustomBlockKeys.UPGRADES, upgrades)

            if (placed.filter.isNotEmpty()) {
                string(CustomBlockKeys.FILTER, StateCodec.encodeFilter(placed.filter))
            }
            if (placed.storage.isNotEmpty()) {
                string(CustomBlockKeys.STORAGE, StateCodec.encodeStorage(placed.storage))
            }
        }
    }

    fun read(item: ItemStack, blockId: String, location: SimpleLocation, ownerUuid: UUID): PlacedBlock? {
        val container = item.itemMeta?.persistentDataContainer ?: return null
        val rawUuid = container.get(CustomBlockKeys.UUID, PersistentDataType.STRING) ?: return null
        val uuid = runCatching { UUID.fromString(rawUuid) }.getOrNull() ?: UUID.randomUUID()

        val upgrades = container.get(CustomBlockKeys.UPGRADES, PersistentDataType.STRING)
            ?.let { StateCodec.decodeUpgrades(it) }
            ?: mutableMapOf()

        val storage = container.get(CustomBlockKeys.STORAGE, PersistentDataType.STRING)
            ?.let { StateCodec.decodeStorage(it) }
            ?: mutableListOf()

        val filter = container.get(CustomBlockKeys.FILTER, PersistentDataType.STRING)
            ?.let { StateCodec.decodeFilter(it) }
            ?: mutableSetOf()

        val filterMode = container.get(CustomBlockKeys.FILTER_MODE, PersistentDataType.STRING)
            ?.let { runCatching { FilterMode.valueOf(it) }.getOrNull() }
            ?: FilterMode.BLACKLIST

        val enabled = (container.get(CustomBlockKeys.ENABLED, PersistentDataType.INTEGER) ?: 1) != 0

        return PlacedBlock(
            uuid = uuid,
            location = location,
            blockId = blockId,
            ownerUuid = ownerUuid,
            enabled = enabled,
            upgrades = upgrades,
            storage = storage,
            filterMode = filterMode,
            filter = filter,
        )
    }
}
