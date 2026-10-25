package ocean_block.system.machines.miner

enum class DrillTier(
    val level: Int,
    val label: String,
    val color: String,
    val ticksPerBlock: Int,
    val itemId: String?,
    val model: String,
) {
    IRON(0, "ferro", "<gray>", 40, null, "minerador_broca"),
    GOLD(1, "ouro", "<gold>", 30, "broca_ouro", "minerador_broca_ouro"),
    DIAMOND(2, "diamante", "<aqua>", 20, "broca_diamante", "minerador_broca_diamante"),
    OBSIDIAN(3, "obsidiana", "<dark_purple>", 15, "broca_obsidiana", "minerador_broca_obsidiana"),
    NETHERITE(4, "netherite", "<dark_gray>", 10, "broca_netherite", "minerador_broca_netherite"),
    ;

    val gain: Int get() = CYCLE_TICKS * MinerRules.BLOCK_UNITS / ticksPerBlock

    val cycleEnergy: Long
        get() = (MinerRules.ENERGY_PER_BLOCK * gain + MinerRules.BLOCK_UNITS - 1) / MinerRules.BLOCK_UNITS

    val energyPerSecond: Long get() = cycleEnergy * TICKS_PER_SECOND / CYCLE_TICKS

    val secondsLabel: String
        get() = (ticksPerBlock / TICKS_PER_SECOND.toDouble()).toString().removeSuffix(".0").replace('.', ',')

    companion object {
        const val KEY = "minerador_broca"
        const val CYCLE_TICKS = 10
        private const val TICKS_PER_SECOND = 20

        fun of(level: Long?): DrillTier = entries.firstOrNull { it.level.toLong() == level } ?: IRON

        fun byItemId(id: String?): DrillTier? = id?.let { entries.firstOrNull { tier -> tier.itemId == it } }
    }
}
