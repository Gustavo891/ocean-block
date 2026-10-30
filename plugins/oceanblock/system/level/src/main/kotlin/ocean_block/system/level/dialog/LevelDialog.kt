package ocean_block.system.level.dialog

import ocean_block.libs.instance.Instance
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.islands.dialog.IslandPortraits
import ocean_block.system.level.PlayerLevels
import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.level.data.LevelManager
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object LevelDialog : Listener {

    private const val SCREEN = "niveis"
    private val HEADER_KEYS = listOf("nivel", "nivel_de", "xp", "barra", "faltam")
    private val PAGER_KEYS = listOf("faixa", "pagina1_estilo", "pagina2_estilo")

    private val pages = ConcurrentHashMap<UUID, Int>()

    fun registerIcons() {
        DialogApi.registerItemIcons(LevelScreenLore.ICONS)
    }

    fun open(player: Player, fallback: (Player) -> Unit) {
        pages[player.uniqueId] = LevelScreenLore.pageOf(PlayerLevels.levelOf(player))
        IslandPortraits.bust(player.playerProfile).thenAccept { bust ->
            Bukkit.getScheduler().runTask(Instance, Runnable { if (player.isOnline) show(player, bust, fallback) })
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        pages.remove(event.player.uniqueId)
    }

    private fun show(player: Player, bust: BufferedImage?, fallback: (Player) -> Unit) {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, bust) }
        if (result == DialogOpenResult.MissingScreen || result == DialogOpenResult.Failed) fallback(player)
    }

    private fun bind(binds: DialogBinds, player: Player, bust: BufferedImage?) {
        val page = { pages[player.uniqueId] ?: 0 }
        val xp = { LevelManager.xp(player.uniqueId) }
        val recipes = { Recipes.all().groupBy { it.level } }
        bust?.let { image -> binds.pixels("jogador") { image } }
        (0 until LevelScreenLore.ROWS).forEach { row ->
            val key = "fileira${row + 1}"
            binds.list(key) { LevelScreenLore.row(page(), row, xp(), recipes()) }
            binds.listPage(key) { 0 }
            binds.listButton("ver${row + 1}") { _, _ -> }
        }
        (0 until LevelScreenLore.ROWS - 1).forEach { row ->
            val key = "desce${row + 1}"
            binds.list(key) { listOf(LevelScreenLore.drop(page(), row, LevelCurve.levelOf(xp()))) }
            binds.listPage(key) { 0 }
        }
        HEADER_KEYS.forEach { key -> binds.text(key) { LevelScreenLore.header(xp())[key].orEmpty() } }
        PAGER_KEYS.forEach { key -> binds.text(key) { LevelScreenLore.pager(page())[key].orEmpty() } }
        binds.list("proximo") { LevelScreenLore.next(xp(), recipes()) }
        binds.listPage("proximo") { 0 }
        binds.list("receita") { LevelScreenLore.nextRecipe(LevelCurve.levelOf(xp()), recipes()) }
        binds.listPage("receita") { 0 }
        binds.button("anterior") { pages[player.uniqueId] = (page() - 1).coerceAtLeast(0) }
        binds.button("proxima") { pages[player.uniqueId] = (page() + 1).coerceAtMost(LevelScreenLore.pages() - 1) }
    }
}
