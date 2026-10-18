package one_block.system.custom_items.content.vacuum_hopper

import one_block.system.custom_items.block.BlockUpgrade

enum class VacuumHopperUpgrade(
    override val id: String,
    override val prettyName: String,
    override val maxLevel: Int,
    override val lore: List<String>,
    private val costs: List<Double>,
) : BlockUpgrade {

    Storage(
        id = "vacuum_storage",
        prettyName = "Armazenamento",
        maxLevel = 3,
        lore = listOf("Aumenta o espaço de", "armazenamento do bloco."),
        costs = listOf(7_500.0, 20_000.0, 50_000.0),
    ) {
        override fun statLine(level: Int) = "<yellow>${storageSlotsFor(level)} slots"
    },

    Radius(
        id = "vacuum_radius",
        prettyName = "Raio",
        maxLevel = 3,
        lore = listOf("Aumenta o alcance", "da sucção."),
        costs = listOf(5_000.0, 15_000.0, 40_000.0),
    ) {
        override fun statLine(level: Int) = "<yellow>${radiusFor(level).toInt()} blocos"
    },

    Speed(
        id = "vacuum_speed",
        prettyName = "Velocidade",
        maxLevel = 3,
        lore = listOf("Diminui o tempo entre", "cada sucção."),
        costs = listOf(10_000.0, 25_000.0, 60_000.0),
    ) {
        override fun statLine(level: Int) = "<yellow>${tickIntervalFor(level)} ticks"
    };

    override fun costFor(level: Int): Double = costs.getOrElse(level) { 0.0 }

    abstract override fun statLine(level: Int): String

    companion object {
        const val BASE_RADIUS = 4.0
        const val RADIUS_PER_LEVEL = 2.0

        const val BASE_STORAGE_SLOTS = 27
        const val STORAGE_SLOTS_PER_LEVEL = 9

        const val BASE_TICK_INTERVAL = 20L
        const val TICK_INTERVAL_PER_LEVEL = 5L

        const val MAX_LEVEL = 3

        const val LIMIT_PER_ISLAND = 5
        const val UPGRADE_CURRENCY = "coins"

        fun radiusFor(level: Int): Double =
            BASE_RADIUS + level.coerceIn(0, MAX_LEVEL) * RADIUS_PER_LEVEL

        fun storageSlotsFor(level: Int): Int =
            BASE_STORAGE_SLOTS + level.coerceIn(0, MAX_LEVEL) * STORAGE_SLOTS_PER_LEVEL

        fun tickIntervalFor(level: Int): Long =
            BASE_TICK_INTERVAL - level.coerceIn(0, MAX_LEVEL) * TICK_INTERVAL_PER_LEVEL
    }
}
