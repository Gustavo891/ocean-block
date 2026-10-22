package ocean_block.system.craft.screen

import ocean_block.system.craft.recipe.CraftResult
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.RecipeCrafter
import ocean_block.system.craft.recipe.RecipeLocks
import ocean_block.system.craft.recipe.Recipes
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class RecipesScreen(private val logger: Logger) {

    private val views = ConcurrentHashMap<UUID, RecipesView>()
    private val warned = ConcurrentHashMap.newKeySet<String>()

    fun open(player: Player) {
        val categories = available()
        if (categories.isEmpty()) {
            player.sendRichMessage("<red>Nenhuma receita disponível.")
            return
        }
        if (categories.size > RecipeEntries.CATEGORIES_PER_PAGE) warnOnce("categorias") {
            "[receitas] ${categories.size} categorias têm receitas, mas a tela mostra só ${RecipeEntries.CATEGORIES_PER_PAGE}; aumente o perPage de 'categorias' em $SCREEN_ID.json"
        }
        when (DialogApi.openResult(player, SCREEN_ID) { it.bind(player) }) {
            DialogOpenResult.Shown, DialogOpenResult.NeedsPack -> Unit
            DialogOpenResult.MissingScreen, DialogOpenResult.Failed -> unavailable(player)
        }
    }

    private fun unavailable(player: Player) {
        player.sendRichMessage("<red>A tela de receitas não está disponível.")
        warnOnce(SCREEN_ID) { "[receitas] a tela '$SCREEN_ID' do Textura não abriu; confira configs/ui/$SCREEN_ID.json e o console do Textura" }
    }

    private fun warnOnce(key: String, message: () -> String) {
        if (warned.add(key)) logger.warning(message())
    }

    fun forget(id: UUID) {
        views.remove(id)
    }

    private fun DialogBinds.bind(player: Player) {
        val id = player.uniqueId
        text("titulo") { category(id).displayName }
        text("titulo_icone") { category(id).icon }
        list("categorias") { RecipeEntries.categories(available(), category(id)) }
        list("receitas") { recipes(id).map { RecipeEntries.card(cardInput(player, it)) } }
        listPage("receitas") { page(id) }
        list("paginacao") { RecipeEntries.pagination(page(id), recipes(id).size) }
        listButton("categoria") { _, index -> select(id, index) }
        listButton("craftar") { _, index -> craft(player, index) }
        button("anterior") { turn(id, -1) }
        button("proxima") { turn(id, 1) }
    }

    private fun available(): List<RecipeCategory> = Recipes.byCategory().keys.toList()

    private fun category(id: UUID): RecipeCategory =
        RecipeEntries.activeCategory(views[id]?.category, available()) ?: RecipeCategory.entries.first()

    private fun recipes(id: UUID): List<OceanRecipe> = Recipes.byCategory()[category(id)].orEmpty()

    private fun page(id: UUID): Int = RecipeEntries.clampPage(views[id]?.page ?: 0, recipes(id).size)

    private fun cardInput(player: Player, recipe: OceanRecipe): CardInput {
        val item = recipe.result()
        return CardInput(
            recipe,
            materialOf(item),
            item.amount,
            RecipeCrafter.shortages(player, recipe),
            RecipeLocks.lockedBy(player, recipe),
            recipe.ingredients.map { RecipeCrafter.count(player, it) },
        )
    }

    private fun select(id: UUID, index: Int) {
        val chosen = available().getOrNull(index) ?: return
        views[id] = RecipesView(chosen, 0)
    }

    private fun turn(id: UUID, delta: Int) {
        val current = category(id)
        views[id] = RecipesView(current, RecipeEntries.clampPage(page(id) + delta, recipes(id).size))
    }

    private fun craft(player: Player, index: Int) {
        val recipe = recipes(player.uniqueId).getOrNull(index) ?: return
        when (val result = RecipeCrafter.craft(player, recipe)) {
            is CraftResult.Crafted -> player.sendRichMessage("<green>Você craftou <white>${result.item.amount}x ${recipe.name}</white>.")
            is CraftResult.Missing -> player.sendRichMessage("<red>${RecipeEntries.missingText(result.shortages)}")
            is CraftResult.Locked -> player.sendRichMessage("<red>Pesquise antes em <white>/pesquisa</white>: ${result.research}")
        }
    }

    companion object {
        const val SCREEN_ID = "receitas"

        fun registerIcon(recipe: OceanRecipe) {
            DialogApi.registerItemIcons(listOf(materialOf(recipe.result())) + recipe.ingredients.mapNotNull { it.icon })
        }

        private fun materialOf(item: ItemStack): String = item.type.key().value()
    }
}
