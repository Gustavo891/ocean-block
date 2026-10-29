package ocean_block.system.missions.notify

import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Sound
import org.bukkit.entity.Player

object MissionNotifier {

    const val ICON_MATERIAL = "map"

    private const val REGION = "avisos"
    private const val CHOOSE_TICKS = 120
    private const val CHOOSE_KEY = "missao_escolher"
    const val CHOOSE_TEXT = "<aqua>Escolha uma diária em /missoes</aqua>"

    fun completed(player: Player) {
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
    }

    fun chooseDaily(player: Player) {
        NotificationApi.push(player, REGION, NotificationLine(CHOOSE_TEXT, "item:$ICON_MATERIAL", CHOOSE_TICKS, CHOOSE_KEY))
    }
}
