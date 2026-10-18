package one_block.system.fly_timer.hud

import one_block.system.islands.hud.IslandHud
import one_block.system.spawn.spawn.SpawnManager
import one_block.textura.api.HudApi
import one_block.textura.hud.HudSecondary
import one_block.textura.hud.HudSide
import org.bukkit.entity.Player

object FlyHud {

    private const val SECOND = 1L
    private const val MINUTE = 60L
    private const val HOUR = 3600L

    fun show(player: Player, remainingSeconds: Long) {
        if (SpawnManager.isAtSpawn(player)) {
            hide(player)
            return
        }

        val text = "<glyph:angel_wings:bossbar> <white>${format(remainingSeconds)}"
        HudApi.showSecondary(player, IslandHud.SLOT, HudSecondary(text = text, side = HudSide.RIGHT))
    }

    fun hide(player: Player) {
        HudApi.hideSecondary(player, IslandHud.SLOT)
    }

    fun format(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        return when {
            seconds >= HOUR -> "${seconds / HOUR}h ${(seconds % HOUR) / MINUTE}m"
            seconds >= MINUTE -> "${seconds / MINUTE}m ${seconds % MINUTE}s"
            else -> "${seconds / SECOND}s"
        }
    }
}
