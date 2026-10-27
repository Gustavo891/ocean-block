package ocean_block.system.pirate_raid.text

object RaidHudText {

    fun line(wave: Int, waves: Int, alive: Int, secondsLeft: Int): String {
        val seconds = secondsLeft.coerceAtLeast(0)
        val clock = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        val pirates = if (alive == 1) "1 pirata" else "$alive piratas"
        return "Invasão pirata · Onda $wave/$waves · $pirates · $clock"
    }
}
