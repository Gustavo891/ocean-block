package ocean_block.system.playtime_rewards.notify

import ocean_block.system.playtime_rewards.model.Milestone
import ocean_block.system.playtime_rewards.text.RewardText
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Sound
import org.bukkit.entity.Player

object RewardNotifier {

    const val ICON_MATERIAL = "clock"

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 100
    private const val KEY = "tempo"

    fun reached(player: Player, milestone: Milestone) {
        push(player, RewardText.reached(milestone))
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f)
    }

    fun pending(player: Player, count: Int) {
        if (count <= 0) return
        push(player, RewardText.pending(count))
    }

    private fun push(player: Player, text: String) {
        NotificationApi.push(player, REGION, NotificationLine(text, "item:$ICON_MATERIAL", DURATION_TICKS, KEY))
    }
}
