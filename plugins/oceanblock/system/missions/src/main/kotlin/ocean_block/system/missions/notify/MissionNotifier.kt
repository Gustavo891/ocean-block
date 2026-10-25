package ocean_block.system.missions.notify

import org.bukkit.Sound
import org.bukkit.entity.Player

object MissionNotifier {

    const val ICON_MATERIAL = "map"

    fun completed(player: Player) {
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
    }
}
