package ocean_block.system.missions.notify

import ocean_block.system.missions.rules.MissionRules
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Sound
import org.bukkit.entity.Player

object MissionNotifier {

    const val ICON_MATERIAL = "map"

    private const val ICON = "item:$ICON_MATERIAL"
    private const val DURATION_TICKS = 100

    fun completed(player: Player, completed: MissionRules.Completed) {
        val text = "<green>Missão concluída:</green> <white>${completed.mission.text}</white>" +
            if (completed.xp > 0) " <gold>+${completed.xp} XP" else ""
        NotificationApi.push(player, "avisos", NotificationLine(text, ICON, DURATION_TICKS, "missao"))
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
    }
}
