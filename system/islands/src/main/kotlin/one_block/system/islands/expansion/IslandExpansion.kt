package one_block.system.islands.expansion

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import one_block.libs.formatter.NumberFormatter
import one_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class IslandExpansion(
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "island"
    override fun getAuthor(): String = "ocoisaruim"
    override fun getVersion(): String = "1.0"

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val island = IslandManager.getIslandByPlayer(player.uniqueId)
        if (island != null) {
            val currentPhase = island.getCurrentPhase() ?: return ""

            val ownerName = Bukkit.getOfflinePlayer(island.ownerUuid).name ?: island.ownerName

            return when(params) {
                "phaseprogress" -> "${NumberFormatter.format(island.grid.toDouble())}/${NumberFormatter.format(currentPhase.blocksToNextPhase.toDouble())}"
                "phasename" -> currentPhase.prettyName
                "player_island" -> ownerName
                else -> super.onPlaceholderRequest(player, params)
            }
        }

       // val wallet = walletManager.find(player.uniqueId) ?: return "aaaaaaa"
        return super.onPlaceholderRequest(player, params)
    }
}