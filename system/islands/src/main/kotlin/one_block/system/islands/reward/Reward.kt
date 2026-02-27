package one_block.system.islands.reward

import org.bukkit.Material

data class Reward(
    val type: RewardType,
    val value: String? = null,
    val material: Material? = null,
    val amount: Int? = null,
)
