package ocean_block.system.battle_pass.config

import ocean_block.system.currencies.Currencies
import org.bukkit.Material
import org.bukkit.entity.Player

enum class BattlePassTier(
    val tier: Int,
    val xpRequired: Int,
    val freeName: String,
    val freeIcon: Material,
    val freeLore: List<String>,
    val premiumName: String,
    val premiumIcon: Material,
    val premiumLore: List<String>,
) {

    TIER_1(1, 10,
        "100 moedas", Material.GOLD_NUGGET, listOf("<gray>Recompensa: <gold>100 moedas"),
        "500 moedas", Material.GOLD_INGOT, listOf("<gray>Recompensa: <gold>500 moedas")
    ) {
        override fun executeFreeReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 100.0)
        override fun executePremiumReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 500.0)
    },

    TIER_2(2, 20,
        "Nenhuma", Material.AIR, emptyList(),
        "1.000 moedas", Material.GOLD_INGOT, listOf("<gray>Recompensa: <gold>1.000 moedas")
    ) {
        override fun executeFreeReward(player: Player) = Unit
        override fun executePremiumReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 1000.0)
    },

    TIER_3(3, 40,
        "200 moedas", Material.GOLD_NUGGET, listOf("<gray>Recompensa: <gold>200 moedas"),
        "Nenhuma", Material.AIR, emptyList()
    ) {
        override fun executeFreeReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 200.0)
        override fun executePremiumReward(player: Player) = Unit
    },

    TIER_4(4, 60,
        "Nenhuma", Material.AIR, emptyList(),
        "Nenhuma", Material.AIR, emptyList()
    ) {
        override fun executeFreeReward(player: Player) = Unit
        override fun executePremiumReward(player: Player) = Unit
    },

    TIER_5(5, 80,
        "500 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>500 moedas"),
        "2.500 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>2.500 moedas")
    ) {
        override fun executeFreeReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 500.0)
        override fun executePremiumReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 2500.0)
    },

    TIER_6(6, 120,
        "Nenhuma", Material.AIR, emptyList(),
        "5.000 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>5.000 moedas")
    ) {
        override fun executeFreeReward(player: Player) = Unit
        override fun executePremiumReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 5000.0)
    },

    TIER_7(7, 160,
        "1.000 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>1.000 moedas"),
        "Nenhuma", Material.AIR, emptyList()
    ) {
        override fun executeFreeReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 1000.0)
        override fun executePremiumReward(player: Player) = Unit
    },

    TIER_8(8, 200,
        "Nenhuma", Material.AIR, emptyList(),
        "Nenhuma", Material.AIR, emptyList()
    ) {
        override fun executeFreeReward(player: Player) = Unit
        override fun executePremiumReward(player: Player) = Unit
    },

    TIER_9(9, 250,
        "1.500 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>1.500 moedas"),
        "7.500 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>7.500 moedas")
    ) {
        override fun executeFreeReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 1500.0)
        override fun executePremiumReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 7500.0)
    },

    TIER_10(10, 350,
        "2.000 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>2.000 moedas"),
        "10.000 moedas", Material.GOLD_BLOCK, listOf("<gray>Recompensa: <gold>10.000 moedas")
    ) {
        override fun executeFreeReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 2000.0)
        override fun executePremiumReward(player: Player) = Currencies.deposit(player.uniqueId, "coins", 10000.0)
    };

    val hasAnyReward: Boolean get() = freeIcon != Material.AIR || premiumIcon != Material.AIR

    abstract fun executeFreeReward(player: Player)

    abstract fun executePremiumReward(player: Player)

    companion object {
        val ordered: List<BattlePassTier> by lazy { entries.sortedBy { it.tier } }

        val maxTier: Int get() = ordered.lastOrNull()?.tier ?: 0

        fun get(tier: Int): BattlePassTier? = ordered.firstOrNull { it.tier == tier }

        fun fromXp(xp: Int): Int {
            var unlocked = 0
            for (data in ordered) {
                if (xp >= data.xpRequired) unlocked = data.tier else break
            }
            return unlocked
        }

        fun xpProgress(xp: Int): Triple<Int, Int, Int> {
            val currentTierNum = fromXp(xp)
            val currentIdx = ordered.indexOfFirst { it.tier == currentTierNum }

            val nextTier = if (currentIdx == -1) ordered.firstOrNull() else ordered.getOrNull(currentIdx + 1)

            if (nextTier == null) return Triple(currentTierNum, 0, 0)

            val currentTier = if (currentIdx != -1) ordered[currentIdx] else null
            val previousXpRequired = currentTier?.xpRequired ?: 0

            val relativeXp = xp - previousXpRequired
            val relativeGoal = nextTier.xpRequired - previousXpRequired

            return Triple(currentTierNum, relativeXp, relativeGoal)
        }
    }
}
