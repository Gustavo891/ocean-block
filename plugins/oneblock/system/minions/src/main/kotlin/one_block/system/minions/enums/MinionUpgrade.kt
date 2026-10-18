package one_block.system.minions.enums

import one_block.libs.formatter.NumberFormatter
import one_block.system.minions.enums.MinionType.*
import org.bukkit.NamespacedKey

enum class MinionUpgrade(
    val id: String,
    val prettyName: String,
    val maxLevel: Int,
    val lore: List<String>,
    val applicableTo: Set<MinionType>,
    private val costs: List<Double>,
) {

    Battery(
        id = "battery",
        prettyName = "Bateria",
        maxLevel = 3,
        lore = listOf(
            "Aumenta a quantidade de",
            "energia do minion."
        ),
        applicableTo = setOf(Miner, Farmer, Terminator),
        costs = listOf(5_000.0, 15_000.0, 40_000.0),
    ) {
        override fun getStatLine(value: Int, maxValue: Int): String {
            return "<aqua>${NumberFormatter.format(value.toDouble())}"
        }

        override fun itemModel(level: Int): NamespacedKey = NamespacedKey("oneblock", "bateria")
    },


    MinerSpeed(
        id = "miner_speed",
        prettyName = "Velocidade",
        maxLevel = 5,
        lore = listOf(
            "Diminui o tempo entre",
            "cada bloco minerado."
        ),
        applicableTo = setOf(Miner),
        costs = listOf(7_500.0, 20_000.0, 45_000.0, 90_000.0, 175_000.0),
    ) {
        override fun getStatLine(value: Int, maxValue: Int): String {
            val percent = ((value.toDouble() / maxValue) * 100).toInt()
            return "<yellow>+${percent}%"
        }

        override fun itemModel(level: Int): NamespacedKey = NamespacedKey.minecraft("diamond_pickaxe")
    },

    MinerStorage(
        id = "miner_storage",
        prettyName = "Armazém",
        maxLevel = 3,
        lore = listOf(
            "Aumenta o espaço de",
            "armazenamento do minion."
        ),
        applicableTo = setOf(Miner, Terminator),
        costs = listOf(10_000.0, 30_000.0, 75_000.0),
    ) {
        override fun getStatLine(value: Int, maxValue: Int): String {
            return "<yellow>${storageSlotsFor(value)} slots"
        }

        override fun itemModel(level: Int): NamespacedKey = NamespacedKey.minecraft("chest")
    },

    TerminatorDamage(
        id = "terminator_damage",
        prettyName = "Dano",
        maxLevel = 5,
        lore = listOf(
            "Aumenta o dano de",
            "cada golpe do minion."
        ),
        applicableTo = setOf(Terminator),
        costs = listOf(8_000.0, 22_000.0, 50_000.0, 100_000.0, 200_000.0),
    ) {
        override fun getStatLine(value: Int, maxValue: Int): String {
            return "<red>${NumberFormatter.format(terminatorDamageFor(value))} de dano"
        }

        override fun itemModel(level: Int): NamespacedKey = NamespacedKey.minecraft("diamond_sword")
    },

    TerminatorLooting(
        id = "terminator_looting",
        prettyName = "Pilhagem",
        maxLevel = 3,
        lore = listOf(
            "Aumenta a quantidade de",
            "itens que os mobs dropam."
        ),
        applicableTo = setOf(Terminator),
        costs = listOf(12_000.0, 35_000.0, 85_000.0),
    ) {
        override fun getStatLine(value: Int, maxValue: Int): String {
            if (value <= 0) return "<gray>Nenhum"
            return "<light_purple>+0 a +$value por drop"
        }

        override fun itemModel(level: Int): NamespacedKey = NamespacedKey.minecraft("emerald")
    },

    TerminatorSpeed(
        id = "terminator_speed",
        prettyName = "Velocidade",
        maxLevel = 5,
        lore = listOf(
            "Diminui o tempo entre",
            "cada golpe do minion."
        ),
        applicableTo = setOf(Terminator),
        costs = listOf(7_500.0, 20_000.0, 45_000.0, 90_000.0, 175_000.0),
    ) {
        override fun getStatLine(value: Int, maxValue: Int): String {
            return "<yellow>1 golpe / ${terminatorAttackPeriodFor(value)}s"
        }

        override fun itemModel(level: Int): NamespacedKey = NamespacedKey.minecraft("sugar")
    };

    fun costFor(level: Int): Double = costs.getOrElse(level) { 0.0 }

    open fun itemModel(level: Int): NamespacedKey? = null

    abstract fun getStatLine(value: Int, maxValue: Int): String

    companion object {
        const val UPGRADE_CURRENCY = "coins"

        const val BATTERY_MAX_LEVEL = 3

        const val STORAGE_MAX_LEVEL = 3
        const val STORAGE_BASE_SLOTS = 9
        const val STORAGE_SLOTS_PER_LEVEL = 10

        fun storageSlotsFor(level: Int): Int =
            STORAGE_BASE_SLOTS + level.coerceIn(0, STORAGE_MAX_LEVEL) * STORAGE_SLOTS_PER_LEVEL

        const val TERMINATOR_BASE_DAMAGE = 4.0
        const val TERMINATOR_DAMAGE_PER_LEVEL = 2.0

        fun terminatorDamageFor(level: Int): Double =
            TERMINATOR_BASE_DAMAGE + level.coerceAtLeast(0) * TERMINATOR_DAMAGE_PER_LEVEL

        const val TERMINATOR_BASE_COOLDOWN_SECONDS = 5

        fun terminatorCooldownFor(level: Int): Int =
            (TERMINATOR_BASE_COOLDOWN_SECONDS - level).coerceAtLeast(0)

        fun terminatorAttackPeriodFor(level: Int): Int = terminatorCooldownFor(level) + 1

        fun valuesFor(type: MinionType): List<MinionUpgrade> {
            return entries.filter { type in it.applicableTo }
        }
    }
}
