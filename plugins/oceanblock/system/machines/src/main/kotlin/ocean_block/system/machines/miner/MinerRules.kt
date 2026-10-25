package ocean_block.system.machines.miner

object MinerRules {

    const val BLOCK_UNITS = 120
    const val ENERGY_PER_BLOCK = 40L
    const val STORAGE = 27
    const val PROGRESS_KEY = "minerador_progress"

    private val ORES = listOf("COAL", "COPPER", "IRON", "GOLD", "REDSTONE", "LAPIS", "EMERALD", "DIAMOND")

    val MINEABLE: Set<String> = setOf("COBBLESTONE", "STONE") +
        ORES.map { "${it}_ORE" } +
        ORES.map { "DEEPSLATE_${it}_ORE" }

    fun canMine(material: String): Boolean = material in MINEABLE

    fun advance(progress: Int, gain: Int): Int = minOf(progress + gain, BLOCK_UNITS + gain - 1)

    fun isDue(progress: Int): Boolean = progress >= BLOCK_UNITS

    fun afterBreak(progress: Int): Int = (progress - BLOCK_UNITS).coerceAtLeast(0)

    fun adopts(material: String): Boolean = material == "NOTE_BLOCK"

    const val SPIN_STEP_DEGREES = 90f

    const val ON_KEY = "minerador_ligado"

    fun isOn(counter: Long?): Boolean = counter != 0L

    fun nextSpin(angle: Float): Float = (angle + SPIN_STEP_DEGREES) % 360f
}
