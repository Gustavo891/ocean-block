package ocean_block.system.machines.miner

object MinerRules {

    const val INTERVAL = 40
    const val ENERGY_PER_BLOCK = 40L
    const val STORAGE = 27
    const val PROGRESS_KEY = "minerador_progress"

    private val ORES = listOf("COAL", "COPPER", "IRON", "GOLD", "REDSTONE", "LAPIS", "EMERALD", "DIAMOND")

    val MINEABLE: Set<String> = setOf("COBBLESTONE", "STONE") +
        ORES.map { "${it}_ORE" } +
        ORES.map { "DEEPSLATE_${it}_ORE" }

    fun canMine(material: String): Boolean = material in MINEABLE

    fun advance(progress: Int, ticks: Int): Int = minOf(progress + ticks, INTERVAL)

    fun isDue(progress: Int): Boolean = progress >= INTERVAL
}
