package ocean_block.system.scoreboard.level

import org.bukkit.entity.Player

data class LevelProgress(val level: Int, val progress: Double)

object AccountLevel {

    private val PLACEHOLDER = LevelProgress(1, 0.0)

    @Volatile
    var source: (Player) -> LevelProgress = { PLACEHOLDER }
}
