package ocean_block.libs.scoreboard

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object ScoreboardApi {

    fun show(player: Player, title: Component, lines: List<SidebarLine>, hideNumbers: Boolean = true) {
        SidebarService.render(player, title, lines, hideNumbers)
    }

    fun hide(player: Player) {
        SidebarService.hide(player)
    }

    fun has(player: Player): Boolean = SidebarService.has(player)
}
