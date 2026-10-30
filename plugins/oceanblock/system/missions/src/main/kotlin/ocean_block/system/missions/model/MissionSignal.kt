package ocean_block.system.missions.model

sealed interface MissionSignal {

    data class Debris(val byHook: Boolean, val type: String = "") : MissionSignal

    data class GeneratorBlock(val material: String) : MissionSignal

    data object GeneratorFormed : MissionSignal

    data class Crafted(val recipeId: String) : MissionSignal

    data class Sold(val value: Long) : MissionSignal

    data class Smelted(val amount: Int) : MissionSignal

    data object MobKilled : MissionSignal

    data class State(val level: Int, val upgrades: Map<String, Int>?, val hasGenerator: Boolean) : MissionSignal
}
