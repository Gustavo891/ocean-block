package one_block.system.skills.skill

import org.bukkit.Material
import org.bukkit.NamespacedKey
import kotlin.math.pow

enum class Skill(
    val id: String,
    val prettyName: String,
    val icon: Material,
    val bonusPerLevel: Double,
    val bonusDescription: String,
    val description: List<String>,
    val xpPerAction: Long,
    val model: NamespacedKey
) {

    Mining(
        id = "mining",
        prettyName = "Mineração",
        icon = Material.DIAMOND_PICKAXE,
        bonusPerLevel = 0.01,
        bonusDescription = "velocidade de quebra",
        description = listOf(
            "Minere blocos para desbloquear receitas",
            "e perks relacionados a mineração.",
        ),
        xpPerAction = 3,
        model = NamespacedKey("oneblock", "skill_mining"),
    ),

    Farming(
        id = "farming",
        prettyName = "Agricultor",
        icon = Material.DIAMOND_HOE,
        bonusPerLevel = 0.01,
        bonusDescription = "crescimento de plantação",
        description = listOf(
            "Colha plantações para desbloquear novas",
            "receitas e novos perks de agricultura.",
        ),
        xpPerAction = 6,
        model = NamespacedKey("oneblock", "skill_farming"),
    ),

    Combat(
        id = "combat",
        prettyName = "Exterminador",
        icon = Material.DIAMOND_SWORD,
        bonusPerLevel = 0.01,
        bonusDescription = "dano causado",
        description = listOf(
            "Mate mobs e players para liberar novos",
            "perks de combate.",
        ),
        xpPerAction = 15,
        model = NamespacedKey("oneblock", "skill_fighting"),
    ),

    Fishing(
        id = "fishing",
        prettyName = "Pescador",
        icon = Material.FISHING_ROD,
        bonusPerLevel = 0.01,
        bonusDescription = "sorte de pesca",
        description = listOf(
            "Fisgue peixes raros e libere perks de",
            "pesca junto com as receitas.",
        ),
        xpPerAction = 40,
        model = NamespacedKey("oneblock", "skill_fishing"),
    );

    fun bonusAt(level: Int): Double = bonusPerLevel * level

    val perks: List<SkillPerk> get() = SkillPerk.of(this)

    companion object {

        const val MAX_LEVEL = 100

        private const val XP_BASE = 1500.0
        private const val XP_EXPONENT = 1.45

        fun byId(id: String): Skill? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }

        fun xpForLevel(level: Int): Long {
            if (level <= 0) return 0
            return (XP_BASE * level.toDouble().pow(XP_EXPONENT)).toLong()
        }

        fun totalXpForLevel(level: Int): Long {
            if (level <= 0) return 0
            return (1..level.coerceAtMost(MAX_LEVEL)).sumOf { xpForLevel(it) }
        }

        fun levelForXp(totalXp: Long): Int {
            var level = 0
            var remaining = totalXp

            while (level < MAX_LEVEL) {
                val cost = xpForLevel(level + 1)
                if (remaining < cost) break
                remaining -= cost
                level++
            }

            return level
        }

        fun xpIntoLevel(totalXp: Long): Long {
            var remaining = totalXp
            var level = 0

            while (level < MAX_LEVEL) {
                val cost = xpForLevel(level + 1)
                if (remaining < cost) break
                remaining -= cost
                level++
            }

            return remaining
        }
    }
}
