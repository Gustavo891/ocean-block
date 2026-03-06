package one_block.system.islands.hud

import kr.toxicity.hud.api.BetterHud
import kr.toxicity.hud.api.BetterHudAPI
import kr.toxicity.hud.api.hud.Hud
import kr.toxicity.hud.api.player.HudPlayer
import one_block.system.islands.island.IslandManager
import org.bukkit.entity.Player


object HudManager {

    fun updatePlayerHud(player: Player) {
        val island = IslandManager.getIslandAt(player.location)
        if(island != null) {
            addHud(player, "phases")
        } else {
            removeHud(player, "phases")
        }
        addHud(player, "default")
    }

    fun addHud(player: Player, hud: String) {
        val hudPlayer = BetterHudAPI.inst().playerManager.getHudPlayer(player.uniqueId) ?: return
        val hud = BetterHudAPI.inst().hudManager.getHud(hud) ?: return
        hud.add(hudPlayer)
        hudPlayer.update()
    }

    fun removeHud(player: Player, hud: String) {
        val hudPlayer = BetterHudAPI.inst().playerManager.getHudPlayer(player.uniqueId) ?: return
        val hud = BetterHudAPI.inst().hudManager.getHud(hud) ?: return
        hud.remove(hudPlayer)
        hudPlayer.update()

    }

}