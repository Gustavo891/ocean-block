package one_block.system.battle_pass.config

import org.bukkit.Material

sealed class ChallengeObjective {

    abstract val goal: Int

    data class KillMob(val mobType: String, override val goal: Int) : ChallengeObjective()

    data class BreakBlock(val material: String, override val goal: Int) : ChallengeObjective()

    data class BreakOneBlock(override val goal: Int) : ChallengeObjective()

    data class SellItem(val material: Material, override val goal: Int) : ChallengeObjective()

    data class CustomCraft(val id: String, override val goal: Int) : ChallengeObjective()

    data class SellValue(override val goal: Int) : ChallengeObjective()

    data class CraftItem(val material: String, override val goal: Int) : ChallengeObjective()
}
