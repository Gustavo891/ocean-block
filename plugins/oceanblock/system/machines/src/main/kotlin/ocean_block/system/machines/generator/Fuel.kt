package ocean_block.system.machines.generator

object Fuel {

    private val TICKS = mapOf(
        "DRIED_KELP" to 400,
        "DRIED_KELP_BLOCK" to 4000,
        "PRISMARINE_SHARD" to 1600,
    )

    fun ticks(material: String): Int? = TICKS[material]
}
