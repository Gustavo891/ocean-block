package ocean_block.system.islands.hud

import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import one_block.textura.api.HudApi
import one_block.textura.hud.HudContent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object IslandHud {

    const val SLOT = "phases"

    fun show(player: Player, island: Island) {
        val content = contentFor(island)
        HudApi.show(player, SLOT, content)
    }

    fun refresh(island: Island) {
        val content = contentFor(island)
        for (player in viewersOf(island)) {
            HudApi.show(player, SLOT, content)
        }
    }

    fun hide(island: Island) {
        for (player in viewersOf(island)) {
            HudApi.hide(player, SLOT)
        }
    }

    private fun contentFor(island: Island): HudContent =
        HudContent(text = "<white>Raft <dark_gray>• <gray>${island.size}x${island.size}", progress = 1f)

    private fun viewersOf(island: Island): List<Player> =
        IslandManager.playerIsland
            .filterValues { it == island.uuid }
            .keys
            .mapNotNull { Bukkit.getPlayer(it) }
}
