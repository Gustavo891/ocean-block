package one_block.system.islands.hud

import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.textura.api.HudApi
import one_block.textura.hud.HudContent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object IslandHud {

    const val SLOT = "phases"

    fun show(player: Player, island: Island) {
        val content = contentFor(island) ?: return
        HudApi.show(player, SLOT, content)
    }

    fun refresh(island: Island) {
        val content = contentFor(island) ?: return
        for (player in viewersOf(island)) {
            HudApi.show(player, SLOT, content)
        }
    }

    fun hide(island: Island) {
        for (player in viewersOf(island)) {
            HudApi.hide(player, SLOT)
        }
    }

    private fun contentFor(island: Island): HudContent? {
        val phase = island.getCurrentPhase() ?: return null
        val target = phase.blocksToNextPhase

        val text = "<white>Fase: <gray>${phase.prettyName} <dark_gray>(${island.grid}/$target)"

        val progress = if (target > 0) island.grid.toFloat() / target.toFloat() else 1f

        return HudContent(text = text, progress = progress.coerceIn(0f, 1f))
    }

    private fun viewersOf(island: Island): List<Player> =
        IslandManager.playerIsland
            .filterValues { it == island.uuid }
            .keys
            .mapNotNull { Bukkit.getPlayer(it) }
}
