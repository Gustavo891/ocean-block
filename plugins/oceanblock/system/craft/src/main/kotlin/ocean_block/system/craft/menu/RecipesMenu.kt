package ocean_block.system.craft.menu

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.craft.recipe.CraftResult
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.RecipeCategory
import ocean_block.system.craft.recipe.RecipeCrafter
import ocean_block.system.craft.recipe.RecipeLocks
import ocean_block.system.craft.recipe.Recipes
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class RecipesMenu : View() {

    private val pagination = computedPaginationState(
        { ctx -> recipes(ctx.player).toMutableList() }
    ) { ctx, builder, _, recipe ->
        builder.renderWith { recipeItem(ctx.player, recipe) }
            .onClick { click -> craft(click, recipe) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(Mini.mm.deserialize(TITLE))
        config.layout(
            "C OOOOOOO",
            "C OOOOOOO",
            "C OOOOOOO",
            "C OOOOOOO",
            "    < >  ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        repeat(RecipeLore.CATEGORY_SLOTS) { index ->
            render.slot(index + 1, 1)
                .renderWith { categoryItem(render.player, available().getOrNull(index)) }
                .onClick { click -> select(click, available().getOrNull(index)) }
        }
        render.layoutSlot('<')
            .renderWith { arrowItem("<yellow>Página anterior") }
            .onClick { click -> pagination.get(click).back() }
        render.layoutSlot('>')
            .renderWith { arrowItem("<yellow>Próxima página") }
            .onClick { click -> pagination.get(click).advance() }
    }

    private fun select(click: SlotClickContext, category: RecipeCategory?) {
        if (category == null) return
        RecipeSelections.set(click.player.uniqueId, category)
        click.player.playSound(click.player, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f)
        refresh(click, firstPage = true)
    }

    private fun craft(click: SlotClickContext, recipe: OceanRecipe) {
        val player = click.player
        when (val result = RecipeCrafter.craft(player, recipe)) {
            is CraftResult.Crafted -> {
                player.sendRichMessage("<green>Você craftou <white>${result.item.amount}x ${recipe.name}</white>.")
                player.playSound(player, Sound.BLOCK_SMITHING_TABLE_USE, 0.7f, 1.2f)
            }
            is CraftResult.Missing -> {
                player.sendRichMessage("<red>${RecipeLore.missingText(result.shortages)}")
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            }
            is CraftResult.Locked -> {
                player.sendRichMessage("<red>Você precisa do nível <white>${result.level}</white> para craftar isso.")
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            }
        }
        refresh(click, firstPage = false)
    }

    private fun refresh(context: Context, firstPage: Boolean) {
        val pages = pagination.get(context)
        if (firstPage) pages.switchTo(0)
        pages.forceUpdate()
        context.update()
    }

    private fun recipeItem(player: Player, recipe: OceanRecipe): ItemStack {
        val result = recipe.result()
        val card = RecipeCard(
            recipe,
            result.amount,
            RecipeCrafter.shortages(player, recipe),
            RecipeLocks.requiredLevel(player, recipe),
            recipe.ingredients.map { RecipeCrafter.count(player, it) },
        )
        return itemStack(result) {
            meta {
                name(RecipeLore.name(card))
                setLore { RecipeLore.lore(card).forEach { +it } }
                addItemFlags(*ItemFlag.entries.toTypedArray())
            }
        }
    }

    private fun categoryItem(player: Player, category: RecipeCategory?): ItemStack {
        if (category == null) return ItemStack(Material.AIR)
        val active = category == category(player)
        val material = Material.matchMaterial(category.material) ?: Material.BOOK
        return itemStack(material) {
            meta {
                name(RecipeLore.categoryName(category, active))
                setLore { RecipeLore.categoryLore(category, active).forEach { +it } }
                if (active) addEnchant(Enchantment.UNBREAKING, 1, true)
                addItemFlags(*ItemFlag.entries.toTypedArray())
            }
        }
    }

    private fun arrowItem(label: String): ItemStack = itemStack(Material.PAPER) {
        meta {
            itemModel = BLANK
            name(label)
        }
    }

    private fun available(): List<RecipeCategory> = Recipes.byCategory().keys.toList()

    private fun category(player: Player): RecipeCategory? =
        RecipeLore.activeCategory(RecipeSelections.get(player.uniqueId), available())

    private fun recipes(player: Player): List<OceanRecipe> =
        category(player)?.let { Recipes.byCategory()[it] }.orEmpty()

    companion object {
        private const val TITLE = "<white><shift:-8><glyph:receitas_menu><font:oneblock:gui><shift:-170><dark_gray>Receitas"
        private val BLANK = NamespacedKey("oneblock", "blank")
    }
}
