package one_block.system.islands.expansion

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import one_block.system.islands.island.IslandManager
import org.bukkit.entity.Player

class IslandExpansion(
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "island"
    override fun getAuthor(): String = "ocoisaruim"
    override fun getVersion(): String = "1.0"

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        var island = IslandManager.getIslandByPlayer(player.uniqueId)
        if (island != null) {
            val currentPhase = island.getCurrentPhase() ?: return ""

            return when(params) {
                "phaseprogress" -> "${island.grid}/${currentPhase.blocksToNextPhase}"
                "phasename" -> "${currentPhase.prettyName}"
                else -> super.onPlaceholderRequest(player, params)
            }
        }

       // val wallet = walletManager.find(player.uniqueId) ?: return "aaaaaaa"
        return super.onPlaceholderRequest(player, params)
    }
}