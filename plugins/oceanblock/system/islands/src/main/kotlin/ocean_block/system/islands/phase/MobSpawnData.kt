package ocean_block.system.islands.phase

const val MOB_SPAWN_CHANCE_PERCENT = 2.0

const val MONSTER_PARTY_CHANCE_PERCENT = 1.0

data class MobSpawnData(
    val weight: Int,
    val min: Int,
    val max: Int,
)
