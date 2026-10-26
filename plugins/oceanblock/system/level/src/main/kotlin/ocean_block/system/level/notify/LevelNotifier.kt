package ocean_block.system.level.notify

import ocean_block.system.level.levelup.LevelUpCard
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

object LevelNotifier : Listener {

    const val ICON_MATERIAL = "experience_bottle"

    private const val XP_DURATION_TICKS = 70
    private const val CARD_REGION = "toasts"
    private const val CARD_TICKS = 100

    private val tallies = mutableMapOf<UUID, XpTally>()

    fun xp(player: Player, amount: Int) {
        if (amount <= 0) return
        val tally = tallies.getOrPut(player.uniqueId) { XpTally(XP_DURATION_TICKS.toLong()) }
        val total = tally.add(amount, Bukkit.getCurrentTick().toLong())
        NotificationApi.push(player, "xp", NotificationLine(XpText.gain(total), null, XP_DURATION_TICKS, "xp"))
    }

    fun levelUp(player: Player, level: Int, coins: Double, unlocked: List<String>) {
        LevelUpCard.lines(level, coins).forEachIndexed { index, line ->
            NotificationApi.push(player, CARD_REGION, NotificationLine(line.text, line.icon, CARD_TICKS, "nivel_$index"))
        }
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        tallies.remove(event.player.uniqueId)
    }
}
