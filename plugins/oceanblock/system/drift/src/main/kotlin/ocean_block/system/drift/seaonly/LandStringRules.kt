package ocean_block.system.drift.seaonly

import org.bukkit.Material
import org.bukkit.entity.EntityType

object LandStringRules {

    private const val FISHING_LOOT = "gameplay/fishing"

    private val SEA_ONLY = setOf(Material.STRING)
    private val DROPPING_MOBS = setOf(EntityType.SPIDER, EntityType.CAVE_SPIDER)
    private val GIFTING_MOBS = setOf(EntityType.CAT)
    private val DROPPING_BLOCKS = setOf(Material.COBWEB)

    fun isSeaOnly(material: Material): Boolean = material in SEA_ONLY

    fun stripsMobDrops(type: EntityType): Boolean = type in DROPPING_MOBS

    fun blocksGift(type: EntityType, material: Material): Boolean = type in GIFTING_MOBS && isSeaOnly(material)

    fun stripsBlockDrops(block: Material): Boolean = block in DROPPING_BLOCKS

    fun stripsLoot(lootTablePath: String): Boolean = !lootTablePath.startsWith(FISHING_LOOT)

    fun <T> withoutSeaOnly(items: List<T>, materialOf: (T) -> Material): List<T> =
        items.filterNot { isSeaOnly(materialOf(it)) }
}
