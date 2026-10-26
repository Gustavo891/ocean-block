package ocean_block.system.level.levelup

object LevelUpRewards {

    private const val COINS_PER_LEVEL = 100.0

    fun coins(from: Int, to: Int): Double = ((from + 1)..to).sumOf { it * COINS_PER_LEVEL }
}
