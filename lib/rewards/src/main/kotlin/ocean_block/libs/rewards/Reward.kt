package ocean_block.libs.rewards

import org.bukkit.Material

data class Reward(
    val type: RewardType,
    val value: String? = null,
    val material: Material? = null,
    val amount: Int? = null,
)
