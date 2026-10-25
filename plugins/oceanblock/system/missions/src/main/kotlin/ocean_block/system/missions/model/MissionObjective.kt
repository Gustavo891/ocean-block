package ocean_block.system.missions.model

sealed interface MissionObjective {

    val goal: Long

    data class CollectDebris(val amount: Int, val hookOnly: Boolean) : MissionObjective {
        override val goal: Long get() = amount.toLong()
    }

    data object BuildGenerator : MissionObjective {
        override val goal: Long get() = 1
    }

    data class BreakGeneratorBlocks(val amount: Int, val material: String?) : MissionObjective {
        override val goal: Long get() = amount.toLong()
    }

    data class Craft(val recipeId: String) : MissionObjective {
        override val goal: Long get() = 1
    }

    data class Smelt(val amount: Int) : MissionObjective {
        override val goal: Long get() = amount.toLong()
    }

    data class CraftAny(val amount: Int) : MissionObjective {
        override val goal: Long get() = amount.toLong()
    }

    data class ReachLevel(val level: Int) : MissionObjective {
        override val goal: Long get() = 1
    }

    data class Sell(val value: Long) : MissionObjective {
        override val goal: Long get() = value
    }

    data class IslandUpgrade(val upgrade: String, val level: Int) : MissionObjective {
        override val goal: Long get() = 1
    }
}
