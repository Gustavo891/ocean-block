package ocean_block.system.level.menu

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.craft.recipe.OceanRecipe
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.level.PlayerLevels
import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.level.data.LevelManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class LevelMenu : View() {

    private val pageState = initialState<Int>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.size(6)
        config.title(Mini.mm.deserialize(TITLE))
    }

    override fun onFirstRender(render: RenderContext) {
        val player = render.player
        val page = (pageState.get(render) ?: 0).coerceIn(0, LevelPath.pages() - 1)
        val xp = LevelManager.xp(player.uniqueId)
        val recipes = Recipes.all().groupBy { it.level }

        LevelPath.SLOTS.forEachIndexed { index, slot ->
            val level = LevelPath.levelAt(page, index)
            if (level > LevelCurve.MAX_LEVEL) return@forEachIndexed
            render.slot(slot).renderWith { tile(level, xp, recipes[level].orEmpty()) }
        }

        if (page > 0) {
            render.slot(PREVIOUS_SLOT).renderWith { arrow("<white>Página anterior") }
                .onClick { click -> click.openForPlayer(LevelMenu::class.java, page - 1) }
        }
        if (page < LevelPath.pages() - 1) {
            render.slot(NEXT_SLOT).renderWith { arrow("<white>Próxima página") }
                .onClick { click -> click.openForPlayer(LevelMenu::class.java, page + 1) }
        }
        render.slot(CLOSE_SLOT).renderWith { close() }.onClick { click -> click.closeForPlayer() }
        render.slot(INFO_SLOT).renderWith { info(player, xp) }
    }

    private fun tile(level: Int, xp: Long, recipes: List<OceanRecipe>): ItemStack {
        val state = LevelTiles.state(level, LevelCurve.levelOf(xp))
        val base = recipes.firstOrNull()?.result()?.clone() ?: ItemStack(Material.PAPER)
        return itemStack(base) {
            amount = level.coerceAtMost(MAX_STACK)
            meta {
                if (recipes.isEmpty()) itemModel = NamespacedKey("oneblock", "nivel_${state.name.lowercase()}")
                setMaxStackSize(MAX_STACK)
                name(LevelTiles.name(level, state))
                setLore { LevelTiles.lore(level, xp, recipes.map { it.name }).forEach { +it } }
                addItemFlags(*ItemFlag.entries.toTypedArray())
            }
        }
    }

    private fun info(player: Player, xp: Long): ItemStack = itemStack(Material.EXPERIENCE_BOTTLE) {
        val header = LevelLore.header(xp)
        val sources = LevelLore.sources(PlayerLevels.config, Recipes.all().map { it.level }.distinct())
        meta {
            name("<gold>${header["nivel"]}")
            setLore {
                +"<gray>${header["xp"]}"
                +"<gray>${header["prox"]}"
                +""
                +"<white>Como ganhar XP:"
                sources.forEach { +"<gray>∙ ${it["texto"]}" }
                +"<gray>∙ Missões: veja /missoes"
            }
            addItemFlags(*ItemFlag.entries.toTypedArray())
        }
    }

    private fun arrow(label: String): ItemStack = itemStack(Material.ARROW) { meta { name(label) } }

    private fun close(): ItemStack = itemStack(Material.BARRIER) { meta { name("<red>Fechar") } }

    private companion object {
        const val TITLE = "<white><shift:-48><glyph:niveis_menu>"
        const val MAX_STACK = 64
        const val PREVIOUS_SLOT = 45
        const val CLOSE_SLOT = 49
        const val INFO_SLOT = 50
        const val NEXT_SLOT = 53
    }
}
