package ocean_block.system.pirate_raid.notify

import one_block.textura.api.HudApi
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Sound
import org.bukkit.entity.Player

object RaidNotifier {

    private const val REGION = "toasts"
    private const val TICKS = 100
    private const val KEY = "piratas"
    private const val HUD_SLOT = "invasao"

    fun started(player: Player) = toast(player, RaidCard.started())

    fun won(player: Player, coins: Int) {
        toast(player, RaidCard.won(coins))
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    fun fled(player: Player) = toast(player, RaidCard.fled())

    fun horn(player: Player) = player.playSound(player.location, Sound.EVENT_RAID_HORN, 0.8f, 1f)

    fun hud(player: Player, text: String) = HudApi.show(player, HUD_SLOT, text)

    fun hideHud(player: Player) = HudApi.hide(player, HUD_SLOT)

    private fun toast(player: Player, lines: List<ToastLine>) {
        lines.forEachIndexed { index, line ->
            NotificationApi.push(player, REGION, NotificationLine(line.text, line.icon, TICKS, "${KEY}_$index"))
        }
    }
}
