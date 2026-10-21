package ocean_block.system.stations.smelter

object Fuel {

    private val EXACT = mapOf(
        "STICK" to 100,
        "COAL" to 1600,
        "CHARCOAL" to 1600,
        "DRIED_KELP_BLOCK" to 4000,
    )

    fun ticks(material: String): Int? = EXACT[material] ?: when {
        material.endsWith("_PLANKS") -> 300
        material.endsWith("_LOG") || material.endsWith("_WOOD") -> 1200
        else -> null
    }
}
