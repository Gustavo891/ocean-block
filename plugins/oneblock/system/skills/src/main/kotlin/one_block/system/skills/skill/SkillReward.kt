package one_block.system.skills.skill

import one_block.libs.rewards.Reward
import one_block.libs.rewards.RewardType
import org.bukkit.Material

data class SkillLevelReward(
    val level: Int,
    val coins: Double,
    val rewards: List<Reward>,
) {

    val isEmpty: Boolean get() = coins <= 0.0 && rewards.isEmpty()
}

object SkillRewards {

    private const val MILESTONE_STEP = 5
    private const val COINS_PER_MILESTONE = 250.0

    private fun item(material: Material, amount: Int) =
        Reward(type = RewardType.Item, material = material, amount = amount)

    private val extras: Map<Skill, Map<Int, List<Reward>>> = mapOf(
        Skill.Mining to mapOf(
            25 to listOf(item(Material.IRON_BLOCK, 16)),
            50 to listOf(item(Material.DIAMOND_BLOCK, 8)),
            75 to listOf(item(Material.NETHERITE_SCRAP, 4)),
            100 to listOf(item(Material.NETHERITE_BLOCK, 2), item(Material.BEACON, 1)),
        ),

        Skill.Farming to mapOf(
            25 to listOf(item(Material.BONE_BLOCK, 16)),
            50 to listOf(item(Material.HAY_BLOCK, 32)),
            75 to listOf(item(Material.GOLDEN_CARROT, 64)),
            100 to listOf(item(Material.ENCHANTED_GOLDEN_APPLE, 4)),
        ),

        Skill.Combat to mapOf(
            25 to listOf(item(Material.GOLDEN_APPLE, 16)),
            50 to listOf(item(Material.DIAMOND_SWORD, 1)),
            75 to listOf(item(Material.TOTEM_OF_UNDYING, 2)),
            100 to listOf(item(Material.NETHERITE_SWORD, 1)),
        ),

        Skill.Fishing to mapOf(
            25 to listOf(item(Material.NAUTILUS_SHELL, 8)),
            50 to listOf(item(Material.HEART_OF_THE_SEA, 1)),
            75 to listOf(item(Material.TRIDENT, 1)),
            100 to listOf(item(Material.CONDUIT, 1)),
        ),
    )

    fun at(skill: Skill, level: Int): SkillLevelReward? {
        if (level <= 0 || level > Skill.MAX_LEVEL) return null

        val coins = if (level % MILESTONE_STEP == 0) COINS_PER_MILESTONE * (level / MILESTONE_STEP) else 0.0
        val rewards = extras[skill]?.get(level).orEmpty()

        val reward = SkillLevelReward(level, coins, rewards)
        return if (reward.isEmpty) null else reward
    }

    fun nextAfter(skill: Skill, fromLevel: Int): SkillLevelReward? =
        ((fromLevel + 1)..Skill.MAX_LEVEL).firstNotNullOfOrNull { at(skill, it) }
}
