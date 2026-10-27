package ocean_block.system.missions.hud

import ocean_block.libs.instance.Instance
import ocean_block.system.missions.data.MissionManager
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText
import one_block.textura.api.HudApi
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object MissionHud {

    private const val SLOT = "missao"
    private const val BANNER_TICKS = 80L

    private val banners = mutableMapOf<UUID, CompletionBanner>()
    private val suspended = mutableSetOf<UUID>()

    fun show(player: Player, state: PlayerMissions) {
        if (player.uniqueId in suspended) return HudApi.hide(player, SLOT)
        val text = banners[player.uniqueId]?.current(now())
            ?: MissionText.hud(MissionRules.current(state), MissionRules.trailDone(state))
        if (text.isEmpty()) return HudApi.hide(player, SLOT)
        HudApi.show(player, SLOT, text)
    }

    fun completed(player: Player, texts: List<String>) {
        if (texts.isEmpty()) return
        val now = now()
        val banner = banners.getOrPut(player.uniqueId) { CompletionBanner(BANNER_TICKS) }
        banner.push(texts, now)
        banner.ends().filter { it > now }.forEach { end ->
            Bukkit.getScheduler().runTaskLater(Instance, Runnable {
                if (player.isOnline) show(player, MissionManager.get(player.uniqueId))
            }, end - now)
        }
    }

    fun suspend(player: Player) {
        suspended.add(player.uniqueId)
        HudApi.hide(player, SLOT)
    }

    fun resume(player: Player) {
        if (!suspended.remove(player.uniqueId)) return
        if (player.isOnline) show(player, MissionManager.get(player.uniqueId))
    }

    fun hide(player: Player) {
        banners.remove(player.uniqueId)
        suspended.remove(player.uniqueId)
        HudApi.hide(player, SLOT)
    }

    private fun now(): Long = Bukkit.getCurrentTick().toLong()
}
