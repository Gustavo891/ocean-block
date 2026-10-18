package one_block.system.collections.collection

import org.bukkit.Material

enum class CollectionEntry(
    val id: String,
    val prettyName: String,
    val icon: Material,
    val materials: Set<Material>,
    val tiers: List<Long>,
) {

    Cobblestone(
        id = "cobblestone",
        prettyName = "Pedregulho",
        icon = Material.COBBLESTONE,
        materials = setOf(Material.COBBLESTONE, Material.STONE, Material.COBBLED_DEEPSLATE, Material.DEEPSLATE),
        tiers = listOf(50, 250, 1_000, 5_000, 25_000, 100_000, 500_000),
    ),

    Coal(
        id = "coal",
        prettyName = "Carvão",
        icon = Material.COAL,
        materials = setOf(Material.COAL, Material.COAL_BLOCK),
        tiers = listOf(50, 250, 1_000, 5_000, 25_000, 100_000),
    ),

    Iron(
        id = "iron",
        prettyName = "Ferro",
        icon = Material.IRON_INGOT,
        materials = setOf(Material.IRON_INGOT, Material.RAW_IRON, Material.IRON_BLOCK),
        tiers = listOf(50, 250, 1_000, 5_000, 25_000, 100_000),
    ),

    Gold(
        id = "gold",
        prettyName = "Ouro",
        icon = Material.GOLD_INGOT,
        materials = setOf(Material.GOLD_INGOT, Material.RAW_GOLD, Material.GOLD_BLOCK),
        tiers = listOf(50, 250, 1_000, 5_000, 25_000, 100_000),
    ),

    Diamond(
        id = "diamond",
        prettyName = "Diamante",
        icon = Material.DIAMOND,
        materials = setOf(Material.DIAMOND, Material.DIAMOND_BLOCK),
        tiers = listOf(25, 100, 500, 2_500, 10_000, 50_000),
    ),

    Emerald(
        id = "emerald",
        prettyName = "Esmeralda",
        icon = Material.EMERALD,
        materials = setOf(Material.EMERALD, Material.EMERALD_BLOCK),
        tiers = listOf(25, 100, 500, 2_500, 10_000, 50_000),
    ),

    Netherite(
        id = "netherite",
        prettyName = "Netherite",
        icon = Material.NETHERITE_SCRAP,
        materials = setOf(Material.NETHERITE_SCRAP, Material.NETHERITE_INGOT, Material.ANCIENT_DEBRIS),
        tiers = listOf(5, 25, 100, 500, 2_500),
    ),

    Wood(
        id = "wood",
        prettyName = "Madeira",
        icon = Material.OAK_LOG,
        materials = setOf(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
        ),
        tiers = listOf(50, 250, 1_000, 5_000, 25_000, 100_000),
    ),

    Wheat(
        id = "wheat",
        prettyName = "Trigo",
        icon = Material.WHEAT,
        materials = setOf(Material.WHEAT, Material.HAY_BLOCK),
        tiers = listOf(50, 250, 1_000, 5_000, 25_000, 100_000),
    ),

    Rotten(
        id = "rotten",
        prettyName = "Carne Podre",
        icon = Material.ROTTEN_FLESH,
        materials = setOf(Material.ROTTEN_FLESH),
        tiers = listOf(50, 250, 1_000, 5_000, 25_000),
    );

    val maxTier: Int get() = tiers.size

    fun tierFor(amount: Long): Int = tiers.count { amount >= it }

    fun amountForTier(tier: Int): Long {
        if (tier <= 0) return 0
        return tiers.getOrElse(tier - 1) { tiers.last() }
    }

    companion object {

        private val materialIndex: Map<Material, CollectionEntry> = buildMap {
            for (entry in values()) {
                for (material in entry.materials) put(material, entry)
            }
        }

        fun byId(id: String): CollectionEntry? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }

        fun byMaterial(material: Material): CollectionEntry? = materialIndex[material]
    }
}
