package ocean_block.system.craft.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.craft.CraftCategory
import ocean_block.system.craft.CraftStation
import ocean_block.system.craft.forge.ForgeRecipeManager

abstract class CategoriesView(private val station: CraftStation) : View() {

    private val pagination = computedPaginationState<CraftCategory>(
        { _ -> ForgeRecipeManager.categoriesOf(station).toMutableList() }
    ) { context, builder, _, value ->
        builder.renderWith { categoryItem(context, value) }
            .onClick { click -> click.openForPlayer(CraftView::class.java, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(station.prettyName)
        config.layout(
            "         ",
            " OOOOOOO ",
            "         ",
        )
    }

    private fun categoryItem(ctx: Context, category: CraftCategory) = itemStack(category.icon) {
        val receitas = ForgeRecipeManager.byCategory(category)
        val disponiveis = receitas.count {
            ForgeRecipeManager.isUnlocked(ctx.player, it) && CraftRequirements.hasAll(ctx.player, it)
        }

        meta {
            name("<yellow>${category.prettyName}")

            addLore {
                +"<gray>${category.description}"
                +""
                +"<dark_gray>Receitas: <gray>${receitas.size}"
                if (disponiveis > 0) {
                    +"<#10FF47>$disponiveis ao seu alcance agora."
                } else {
                    +"<#FF1E20>Nenhuma ao seu alcance ainda."
                }
                +""
                +"<yellow>Clique para abrir."
            }
        }
    }
}

class CraftCategoriesView : CategoriesView(CraftStation.CRAFT)

class ForgeCategoriesView : CategoriesView(CraftStation.FORJA)
