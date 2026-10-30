package ocean_block.system.missions.dialog

import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.missions.MissionTracker
import ocean_block.system.missions.data.MissionManager
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TrailDialog {

    private const val SCREEN = "trilha"

    private val selections = ConcurrentHashMap<UUID, Int>()

    fun open(player: Player): Boolean {
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
        val selected = { TrailLore.selection(state(), selections[player.uniqueId]) }
        (0 until TrailLore.ROWS).forEach { row ->
            val key = "fileira${row + 1}"
            binds.list(key) { TrailLore.row(state(), row, selected()) }
            binds.listPage(key) { 0 }
            binds.listButton("ver${row + 1}") { _, position -> TrailLore.indexAt(row, position)?.let { selections[player.uniqueId] = it } }
        }
        (0 until TrailLore.ROWS - 1).forEach { row ->
            val key = "desce${row + 1}"
            binds.list(key) { listOf(TrailLore.drop(state(), row)) }
            binds.listPage(key) { 0 }
        }
        val detail = { TrailLore.detail(state(), selected(), ::recipeName) }
        binds.list("detalhe") { listOf(detail()) }
        binds.listPage("detalhe") { 0 }
        binds.list("recompensas") { TrailLore.rewards(selected()) }
        binds.listPage("recompensas") { 0 }
        listOf("botao", "bloqueado", "dica", "nota").forEach { key -> binds.text(key) { detail()[key].orEmpty() } }
        binds.button("escolher") { }
        binds.button("diarias") { click -> MissionDialog.openDailies(click.player) }
        binds.text("resumo") { TrailLore.summary(state()) }
        binds.text("xp") { TrailLore.xp(state()) }
        binds.text("libera") { TrailLore.remaining(state()) }
        binds.text("diarias_estado") { TrailLore.dailies(state()) }
    }

    private fun recipeName(id: String): String = Recipes.get(id)?.name ?: id
}
