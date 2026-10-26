package ocean_block.system.level.notify

import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import ocean_block.libs.formatter.Mini
import ocean_block.libs.instance.Instance
import ocean_block.system.level.levelup.LevelUpScreen
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.UUID

object LevelNotifier : Listener {

    const val ICON_MATERIAL = "experience_bottle"

    private const val XP_DURATION_TICKS = 70
    private const val FRAME_TICKS = 3L
    private const val FRAME_STEPS = 10
    private val TIMES = Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(3000), Duration.ofMillis(1000))

    private val tallies = mutableMapOf<UUID, XpTally>()
    private val animations = mutableMapOf<UUID, BukkitTask>()

    fun xp(player: Player, amount: Int) {
        if (amount <= 0) return
        val tally = tallies.getOrPut(player.uniqueId) { XpTally(XP_DURATION_TICKS.toLong()) }
        val total = tally.add(amount, Bukkit.getCurrentTick().toLong())
        NotificationApi.push(player, "xp", NotificationLine(XpText.gain(total), null, XP_DURATION_TICKS, "xp"))
    }

    fun levelUp(player: Player, level: Int, coins: Double, unlocked: List<String>) {
        fun subtitle(frame: Int) = Mini.mm.deserialize(LevelUpScreen.subtitle(level, frame, coins, unlocked))
        player.showTitle(Title.title(Mini.mm.deserialize(LevelUpScreen.title()), subtitle(0), TIMES))
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.3f)

        animations.remove(player.uniqueId)?.cancel()
        var step = 1
        animations[player.uniqueId] = Bukkit.getScheduler().runTaskTimer(Instance, Runnable {
            if (!player.isOnline || step > FRAME_STEPS) {
                animations.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            player.sendTitlePart(TitlePart.SUBTITLE, subtitle(step % LevelUpScreen.FRAMES))
            step++
        }, FRAME_TICKS, FRAME_TICKS)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        tallies.remove(event.player.uniqueId)
        animations.remove(event.player.uniqueId)?.cancel()
    }
}
