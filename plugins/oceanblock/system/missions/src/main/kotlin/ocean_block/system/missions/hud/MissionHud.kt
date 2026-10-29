package ocean_block.system.missions.hud

import ocean_block.libs.instance.Instance
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.missions.data.MissionManager
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionCard
import ocean_block.system.missions.text.MissionCardText
import one_block.textura.api.HudApi
import one_block.textura.hud.HudCard
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object MissionHud {

    private const val SLOT = "missao"
    private const val BACKGROUND = "hud_missao"
    private const val BANNER_TICKS = 80L

    private const val BAR = "hud_mare"

    private val banners = mutableMapOf<UUID, CompletionBanner<MissionCardText>>()
    private val suspended = mutableSetOf<UUID>()

    fun show(player: Player, state: PlayerMissions) {
        if (player.uniqueId in suspended) return HudApi.hide(player, SLOT)
        val card = banners[player.uniqueId]?.current(now())
            ?: MissionCard.current(MissionRules.current(state), ::recipeName)
            ?: return HudApi.hide(player, SLOT)
        HudApi.showCard(player, SLOT, HudCard(card.topLeft, card.topRight, card.bottomRight, card.progress, BAR), BACKGROUND)
    }

    fun completed(player: Player, texts: List<MissionCardText>) {
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

    private fun recipeName(id: String): String = Recipes.get(id)?.name ?: id

    private fun now(): Long = Bukkit.getCurrentTick().toLong()
}
