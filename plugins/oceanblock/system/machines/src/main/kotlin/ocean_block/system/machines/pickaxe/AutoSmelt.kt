package ocean_block.system.machines.pickaxe

object AutoSmelt {

    private val RESULTS = mapOf(
        "RAW_IRON" to "IRON_INGOT",
        "RAW_COPPER" to "COPPER_INGOT",
        "RAW_GOLD" to "GOLD_INGOT",
        "COBBLESTONE" to "STONE",
    )

    fun smelt(material: String): String? = RESULTS[material]
}
