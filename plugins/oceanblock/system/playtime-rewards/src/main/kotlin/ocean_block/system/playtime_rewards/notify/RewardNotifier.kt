package ocean_block.system.playtime_rewards.notify

import ocean_block.system.playtime_rewards.model.Milestone
import ocean_block.system.playtime_rewards.text.RewardText
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Sound
import org.bukkit.entity.Player

object RewardNotifier {

    const val ICON_MATERIAL = "clock_00"

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 100
    private const val KEY = "tempo"
    private const val CARD_REGION = "toasts"
    private const val CARD_TICKS = 100

    fun reached(player: Player, milestone: Milestone) {
        RewardCard.lines(milestone.minutes).forEachIndexed { index, line ->
            NotificationApi.push(player, CARD_REGION, NotificationLine(line.text, line.icon, CARD_TICKS, "tempo_$index"))
        }
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    fun pending(player: Player, count: Int) {
        if (count <= 0) return
        NotificationApi.push(player, REGION, NotificationLine(RewardText.pending(count), "item:$ICON_MATERIAL", DURATION_TICKS, KEY))
    }
}
