package ocean_block.system.pirate_raid.rules

object RaidRoll {

    fun eligible(active: Boolean, lastDay: String?, today: String, maxLevel: Int, minLevel: Int): Boolean =
        !active && lastDay != today && maxLevel >= minLevel

    fun hits(roll: Double, chance: Double): Boolean = roll < chance
}
