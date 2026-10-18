package one_block.system.spawners.models

import org.bukkit.Material
import org.bukkit.entity.EntityType

enum class SpawnerType(
    val entityType: EntityType,
    val headOwner: String,
    val headTexture: String,
    val displayName: String,
    val price: Double,
    val spawnIntervalTicks: Long,
    val maxLiveMobs: Int,
    val xpPerKill: Int,
    val loot: List<LootEntry>,
) {

    COW(
        entityType = EntityType.COW,
        headOwner = "MHF_Cow",
        headTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGY4Y2UzOTE1YTIxMTY4ZDgzOTQyMTQ1NTJjZGI1NjUyZTg1NWU5ZTM4MjAwMWQ1OTY5NzJmZTNjMDA5ZWE3ZCJ9fX0=",
        displayName = "Vaca",
        price = 5000.0,
        spawnIntervalTicks = 200L,
        maxLiveMobs = 500,
        xpPerKill = 5,
        loot = listOf(
            LootEntry(Material.LEATHER, min = 1, max = 2, chance = 1.0),
            LootEntry(Material.BEEF, min = 1, max = 2, chance = 1.0),
        )
    );

    companion object {
        fun find(id: String): SpawnerType = entries.first() { it.name == id }
    }


}
