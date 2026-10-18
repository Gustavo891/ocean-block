package one_block.textura.dialog.text

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Papi {

    private const val PLUGIN = "PlaceholderAPI"

    fun resolve(player: Player?, identifier: String): String? {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled(PLUGIN)) return null
        val token = "%$identifier%"
        return PlaceholderAPI.setPlaceholders(player, token).takeIf { it != token }
    }
}
