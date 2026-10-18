package ocean_block.system.enchants.enchant

import org.bukkit.Material

enum class EnchantTarget(
    val prettyName: String,
    private val suffixes: Set<String>,
) {

    Pickaxe("Picaretas", setOf("_PICKAXE")),
    Axe("Machados", setOf("_AXE")),
    Shovel("Pás", setOf("_SHOVEL")),
    Hoe("Enxadas", setOf("_HOE")),
    Sword("Espadas", setOf("_SWORD")),
    Bow("Arcos", setOf("BOW", "CROSSBOW")),
    Helmet("Capacetes", setOf("_HELMET")),
    Chestplate("Peitorais", setOf("_CHESTPLATE")),
    Leggings("Calças", setOf("_LEGGINGS")),
    Boots("Botas", setOf("_BOOTS"));

    fun matches(material: Material): Boolean = suffixes.any { material.name.endsWith(it) }

    companion object {
        val TOOLS = setOf(Pickaxe, Axe, Shovel, Hoe)
        val ARMOR = setOf(Helmet, Chestplate, Leggings, Boots)
        val WEAPONS = setOf(Sword, Bow)
    }
}
