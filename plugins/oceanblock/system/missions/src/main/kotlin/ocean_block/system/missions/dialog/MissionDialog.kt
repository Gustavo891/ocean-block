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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object MissionDialog {

    private const val SCREEN = "missoes"
    private const val ENTITY = "entidade:"
    private val EXTRA_ICONS = listOf("nether_star", "experience_bottle", "map")

    fun registerIcons() {
        val icons = ((MissionTrail.all + DailyPool.all).map { it.icon } + EXTRA_ICONS).distinct()
        DialogApi.registerItemIcons(icons.filter { !it.contains(':') })
        DialogApi.registerEntityIcons(icons.filter { it.startsWith(ENTITY) }.map { it.removePrefix(ENTITY) })
    }

    private val selections = ConcurrentHashMap<UUID, String>()

    fun open(player: Player): Boolean {
        MissionTracker.refresh(player)
        if (!MissionRules.trailDone(MissionManager.get(player.uniqueId))) return TrailDialog.open(player)
        return openDailies(player)
    }

    fun openDailies(player: Player): Boolean {
        MissionTracker.refresh(player)
        selections.remove(player.uniqueId)
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    fun forget(player: Player) {
        selections.remove(player.uniqueId)
    }

    private fun bind(binds: DialogBinds, player: Player) {
        val state = { MissionManager.get(player.uniqueId) }
        val selected = { MissionLore.selection(state(), selections[player.uniqueId]) }
        binds.list("aviso") { listOfNotNull(MissionLore.notice(state())) }
        binds.listPage("aviso") { 0 }
        binds.list("diarias") { state().let { now -> MissionLore.listed(now).map { MissionLore.row(it, now, selected()) } } }
        binds.listPage("diarias") { 0 }
        binds.list("legenda") { MissionLore.legend(state()) }
        binds.listPage("legenda") { 0 }
        binds.listButton("selecionar") { _, index -> MissionLore.listed(state()).getOrNull(index)?.let { selections[player.uniqueId] = it.id } }
        val detail = { MissionLore.detail(state(), selected(), ::recipeName) }
        binds.list("detalhe") { listOf(detail()) }
        binds.listPage("detalhe") { 0 }
        binds.list("recompensas") { MissionLore.rewards(state(), selected()) }
        binds.listPage("recompensas") { 0 }
        listOf("botao", "bloqueado", "dica", "nota").forEach { key -> binds.text(key) { detail()[key].orEmpty() } }
        binds.button("escolher") { click -> selected().takeIf { it != MissionLore.TRAIL }?.let { MissionTracker.choose(click.player, it) } }
        binds.button("trilha") { click -> TrailDialog.open(click.player) }
        binds.text("resumo") { MissionLore.summary(state()) }
        binds.text("diarias_feitas") { MissionLore.dailiesDone(state()) }
        binds.text("trilha") { MissionLore.trailCount(state()) }
        binds.text("trilha_aberta") { (selected() == MissionLore.TRAIL).toString() }
        binds.text("renova") { MissionLore.resetIn(LocalDateTime.now(MissionTracker.zone)) }
    }

    private fun recipeName(id: String): String = Recipes.get(id)?.name ?: id
}
