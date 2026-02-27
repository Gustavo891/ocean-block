package one_block.system.playtime.expansion

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import one_block.libs.formatter.TimeFormatter
import one_block.system.playtime.player_playtime.PlayerPlaytimeManager
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

class PlayerPlaytimeExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "playtime"
    }

    override fun getAuthor(): String {
        return "BADoff"
    }

    override fun getVersion(): String {
        return "1.0"
    }

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val playerPlaytime = PlayerPlaytimeManager.get(player.uniqueId) ?: return null
        return TimeFormatter.formatReduced(TimeUnit.SECONDS.toMillis(playerPlaytime.getElapsedSeconds()))
    }
}