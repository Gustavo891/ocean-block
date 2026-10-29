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
    private const val ENTITY = "entidade:"
    private val EXTRA_ICONS = listOf("nether_star", "experience_bottle", "map")

    fun registerIcons() {
        val icons = ((MissionTrail.all + DailyPool.all).map { it.icon } + EXTRA_ICONS).distinct()
        DialogApi.registerItemIcons(icons.filter { !it.contains(':') })
        DialogApi.registerEntityIcons(icons.filter { it.startsWith(ENTITY) }.map { it.removePrefix(ENTITY) })
    }

    fun open(player: Player): Boolean {
        MissionTracker.refresh(player)
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    private fun bind(binds: DialogBinds, player: Player) {
        val state = { MissionManager.get(player.uniqueId) }
        binds.list("ativa") { listOfNotNull(MissionLore.active(state(), ::recipeName)) }
        binds.listPage("ativa") { 0 }
        binds.list("aviso") { listOfNotNull(MissionLore.notice(state())) }
        binds.listPage("aviso") { 0 }
        binds.list("titulo_diarias") { if (MissionLore.listed(state()).isEmpty()) emptyList() else listOf(emptyMap()) }
        binds.listPage("titulo_diarias") { 0 }
        binds.list("diarias") { MissionLore.listed(state()).map { MissionLore.row(it, ::recipeName) } }
        binds.listPage("diarias") { 0 }
        binds.listButton("escolher") { click, index -> MissionLore.listed(state()).getOrNull(index)?.let { MissionTracker.choose(click.player, it.id) } }
        binds.text("resumo") { MissionLore.summary(state()) }
        binds.text("renova") {
            if (MissionRules.trailDone(state())) MissionLore.resetIn(LocalDateTime.now(MissionTracker.zone)) else ""
        }
    }

    private fun recipeName(id: String): String = Recipes.get(id)?.name ?: id
}
