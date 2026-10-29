package ocean_block.system.missions.dialog

import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.missions.MissionTracker
import ocean_block.system.missions.catalog.DailyPool
import ocean_block.system.missions.catalog.MissionTrail
import ocean_block.system.missions.data.MissionManager
import ocean_block.system.missions.rules.MissionRules
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.entity.Player
import java.time.LocalDateTime

object MissionDialog {

    private const val SCREEN = "missoes"
    private val EXTRA_ICONS = listOf("nether_star", "experience_bottle", "map")

    fun registerIcons() {
        DialogApi.registerItemIcons(((MissionTrail.all + DailyPool.all).map { it.icon } + EXTRA_ICONS).distinct())
    }

    fun open(player: Player): Boolean {
        MissionTracker.refresh(player)
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    private fun bind(binds: DialogBinds, player: Player) {
        val state = { MissionManager.get(player.uniqueId) }
        binds.list("trilha") { listOf(MissionLore.trail(state(), ::recipeName)) }
        binds.listPage("trilha") { 0 }
        binds.list("diarias") { state().let { now -> now.dailies.map { MissionLore.daily(it, it.id == now.activeDaily, ::recipeName) } } }
        binds.listButton("escolher") { click, index -> state().dailies.getOrNull(index)?.let { MissionTracker.choose(click.player, it.id) } }
        binds.listPage("diarias") { 0 }
        binds.text("renova") {
            if (MissionRules.trailDone(state())) MissionLore.resetIn(LocalDateTime.now(MissionTracker.zone))
            else MissionLore.LOCKED_DAILIES
        }
    }

    private fun recipeName(id: String): String = Recipes.get(id)?.name ?: id
}
