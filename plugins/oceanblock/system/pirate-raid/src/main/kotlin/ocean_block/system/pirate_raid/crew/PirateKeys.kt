package ocean_block.system.pirate_raid.crew

import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

enum class PirateKind { PILLAGER, BRUTE, SUPER }

object PirateKeys {

    private const val NAMESPACE = "oceanblock"

    val ISLAND = NamespacedKey(NAMESPACE, "pirata")
    val KIND = NamespacedKey(NAMESPACE, "pirata_tipo")
    val BOAT = NamespacedKey(NAMESPACE, "pirata_barco")
    val COIN = NamespacedKey(NAMESPACE, "raid_moeda")
    val PEARL = NamespacedKey(NAMESPACE, "raid_perola")
    val LOOT_ISLAND = NamespacedKey(NAMESPACE, "raid_ilha")

    fun islandOf(entity: Entity): UUID? =
        entity.persistentDataContainer.get(ISLAND, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    fun kindOf(entity: Entity): PirateKind? =
        entity.persistentDataContainer.get(KIND, PersistentDataType.STRING)?.let { name -> PirateKind.entries.firstOrNull { it.name == name } }

    fun isPirate(entity: Entity): Boolean = entity.persistentDataContainer.has(ISLAND, PersistentDataType.STRING)

    fun isRaidBoat(entity: Entity): Boolean = entity.persistentDataContainer.has(BOAT, PersistentDataType.BYTE)

    fun coinValue(item: ItemStack): Int? = item.persistentDataContainer.get(COIN, PersistentDataType.INTEGER)

    fun pearlValue(item: ItemStack): Int? = item.persistentDataContainer.get(PEARL, PersistentDataType.INTEGER)

    fun lootIsland(item: ItemStack): UUID? =
        item.persistentDataContainer.get(LOOT_ISLAND, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    fun isRaidLoot(item: ItemStack): Boolean = coinValue(item) != null || pearlValue(item) != null
}
