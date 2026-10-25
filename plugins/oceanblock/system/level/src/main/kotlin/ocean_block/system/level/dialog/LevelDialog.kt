package ocean_block.system.level.dialog

import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.level.PlayerLevels
import ocean_block.system.level.data.LevelManager
import ocean_block.system.level.notify.LevelNotifier
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object LevelDialog {

    private const val SCREEN = "nivel"
    private const val PER_PAGE = 10
    private const val FALLBACK_ICON = "crafting_table"

    @Volatile
    private var icons: Map<String, String> = emptyMap()

    fun registerIcons() {
        icons = Recipes.all().associate { it.id to iconOf(it) }
        val sources = LevelLore.sources(PlayerLevels.config, emptyList()).mapNotNull { it["icone"] }
        DialogApi.registerItemIcons((icons.values + sources + LevelNotifier.ICON_MATERIAL + FALLBACK_ICON).distinct())
    }

    fun open(player: Player): Boolean {
        var page = 0
        val result = DialogApi.openResult(player, SCREEN) { binds ->
            bind(binds, player, { page }, { page = it })
        }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    private fun recipes(): List<OceanRecipe> = Recipes.all().sortedWith(compareBy(OceanRecipe::level, OceanRecipe::name))

    private fun pages(): Int = ((recipes().size + PER_PAGE - 1) / PER_PAGE).coerceAtLeast(1)

    private fun bind(binds: DialogBinds, player: Player, page: () -> Int, setPage: (Int) -> Unit) {
        val header = { LevelLore.header(LevelManager.xp(player.uniqueId)) }
        listOf("nivel", "xp", "prox", "pct", "barra").forEach { key -> binds.text(key) { header()[key].orEmpty() } }
        binds.list("fontes") { LevelLore.sources(PlayerLevels.config, recipes().map(OceanRecipe::level)) }
        binds.listPage("fontes") { 0 }
        binds.list("receitas") {
            val level = PlayerLevels.levelOf(player)
            recipes().map { LevelLore.recipeEntry(it, level, icons[it.id] ?: FALLBACK_ICON) }
        }
        binds.listPage("receitas") { page().coerceIn(0, pages() - 1) }
        binds.text("liberadas") {
            val level = PlayerLevels.levelOf(player)
            val all = recipes()
            "${all.count { it.level <= level }} de ${all.size} liberadas"
        }
        binds.text("pagina") { "Página ${page().coerceIn(0, pages() - 1) + 1} de ${pages()}" }
        binds.text("sem_anterior") { (page() <= 0).toString() }
        binds.text("sem_proxima") { (page() >= pages() - 1).toString() }
        binds.button("anterior") { setPage((page() - 1).coerceAtLeast(0)) }
        binds.button("proxima") { setPage((page() + 1).coerceAtMost(pages() - 1)) }
    }

    private fun iconOf(recipe: OceanRecipe): String =
        runCatching { recipe.result().type.name.lowercase() }
            .onFailure { Bukkit.getLogger().warning("[nivel] Ícone da receita '${recipe.id}' indisponível: ${it.message}") }
            .getOrDefault(FALLBACK_ICON)
}
