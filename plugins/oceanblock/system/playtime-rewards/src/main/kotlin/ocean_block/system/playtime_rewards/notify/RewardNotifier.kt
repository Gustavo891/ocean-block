package ocean_block.system.playtime_rewards.notify

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import ocean_block.libs.formatter.Mini
import ocean_block.libs.instance.Instance
import ocean_block.system.playtime_rewards.model.Milestone
import ocean_block.system.playtime_rewards.text.RewardText
import one_block.textura.api.GlyphApi
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

object RewardNotifier : Listener {

    const val ICON_MATERIAL = "clock"

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 100
    private const val KEY = "tempo"
    private const val BANNER_REGION = "tempo"
    private const val BANNER_LINE_TICKS = 70
    private const val FRAME_TICKS = 3L
    private const val FRAME_STEPS = 10
    private val TIMES = Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(3000), Duration.ofMillis(1000))

    private val animations = mutableMapOf<UUID, BukkitTask>()

    fun reached(player: Player, milestone: Milestone) {
        fun title(frame: Int) = flat(RewardBanner.title(milestone.minutes, frame, GlyphApi::has))
        player.showTitle(Title.title(title(0), Component.empty(), TIMES))
        RewardBanner.lines().forEachIndexed { index, line ->
            NotificationApi.push(player, BANNER_REGION, NotificationLine(line, null, BANNER_LINE_TICKS, "tempo_$index"))
        }
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f)
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.4f)

        animations.remove(player.uniqueId)?.cancel()
        var step = 1
        animations[player.uniqueId] = Bukkit.getScheduler().runTaskTimer(Instance, Runnable {
            if (!player.isOnline || step > FRAME_STEPS) {
                animations.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            player.sendTitlePart(TitlePart.TITLE, title(step % RewardBanner.FRAMES))
            step++
        }, FRAME_TICKS, FRAME_TICKS)
    }

    fun pending(player: Player, count: Int) {
        if (count <= 0) return
        NotificationApi.push(player, REGION, NotificationLine(RewardText.pending(count), "item:$ICON_MATERIAL", DURATION_TICKS, KEY))
    }

    private fun flat(text: String): Component = Mini.mm.deserialize(text).shadowColor(ShadowColor.none())

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        animations.remove(event.player.uniqueId)?.cancel()
    }
}
