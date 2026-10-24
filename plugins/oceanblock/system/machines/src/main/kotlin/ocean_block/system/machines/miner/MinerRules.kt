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

    const val HITS_TO_BREAK = 3
    const val HIT_WINDOW_TICKS = 30

    fun adopts(material: String): Boolean = material == "NOTE_BLOCK"

    const val SPIN_STEP_DEGREES = 90f

    const val ON_KEY = "minerador_ligado"

    fun cycleEnergy(ticks: Int): Long = ENERGY_PER_BLOCK * ticks / INTERVAL

    fun isOn(counter: Long?): Boolean = counter != 0L

    fun nextSpin(angle: Float): Float = (angle + SPIN_STEP_DEGREES) % 360f

    fun hits(previous: MinerHits?, now: Int): Int =
        if (previous != null && now - previous.lastTick <= HIT_WINDOW_TICKS) previous.count + 1 else 1
}

data class MinerHits(val count: Int, val lastTick: Int)
