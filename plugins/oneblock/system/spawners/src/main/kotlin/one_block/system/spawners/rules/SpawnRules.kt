package one_block.system.spawners.rules

object SpawnRules {

    fun shouldSpawn(enabled: Boolean, current: Int, maxLive: Int): Boolean =
        enabled && current < maxLive

    fun nextLiveMobs(current: Int, stackSize: Int, maxLive: Int): Int =
        (current + stackSize).coerceAtMost(maxLive)

    fun afterKills(current: Int, kills: Int): Int = (current - kills).coerceAtLeast(0)
}
