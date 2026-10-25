package ocean_block.system.missions.hud

import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText
import one_block.textura.api.HudApi
import org.bukkit.entity.Player

object MissionHud {

    private const val SLOT = "missao"

    fun show(player: Player, state: PlayerMissions) {
        val text = MissionText.hud(MissionRules.current(state), MissionRules.trailDone(state))
        if (text.isEmpty()) return hide(player)
        HudApi.show(player, SLOT, text)
    }

    fun hide(player: Player) {
        HudApi.hide(player, SLOT)
    }
}
