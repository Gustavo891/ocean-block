package ocean_block.system.islands.choice

import net.kyori.adventure.text.Component
import ocean_block.libs.instance.Instance
import ocean_block.libs.scoreboard.ScoreboardApi
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.WeatherType
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

object ChoiceCamera {

    private const val WORLD = "world"
    private const val X = 0.5
    private const val Z = 0.5
    private const val NOON = 6000L

    private val MARK: NamespacedKey by lazy { NamespacedKey(Instance, "escolhendo_ilha") }

    fun location(): Location? =
        Bukkit.getWorld(WORLD)?.let { Location(it, X, ChoiceRules.CAMERA_Y, Z, 0f, ChoiceRules.CAMERA_PITCH) }

    fun enter(player: Player) {
        player.persistentDataContainer.set(MARK, PersistentDataType.BYTE, 1)
        player.gameMode = GameMode.SPECTATOR
        player.setPlayerTime(NOON, false)
        player.setPlayerWeather(WeatherType.CLEAR)
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        ScoreboardApi.suppress(player)
        repeat(ChoiceRules.CHAT_CLEAR_LINES) { player.sendMessage(Component.empty()) }
        location()?.let { player.teleport(it) }
        Bukkit.getPluginManager().callEvent(IslandChoiceEvent(player, true))
    }

    fun leave(player: Player) {
        if (!isMarked(player)) return
        player.persistentDataContainer.remove(MARK)
        player.gameMode = GameMode.SURVIVAL
        player.resetPlayerTime()
        player.resetPlayerWeather()
        ScoreboardApi.release(player)
        Bukkit.getPluginManager().callEvent(IslandChoiceEvent(player, false))
    }

    fun isMarked(player: Player): Boolean = player.persistentDataContainer.has(MARK, PersistentDataType.BYTE)
}
