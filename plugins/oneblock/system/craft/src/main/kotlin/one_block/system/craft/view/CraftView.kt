package one_block.system.craft.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.craft.CraftCategory
import one_block.system.craft.events.CustomCraftEvent
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRecipeManager
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import one_block.libs.formatter.Mini

class CraftView : View() {

    private val categoryState = initialState<CraftCategory>()

    private val pagination = computedPaginationState<ForgeRecipe>(
        { ctx -> ForgeRecipeManager.byCategory(categoryState.get(ctx)).toMutableList() }
    ) { context, builder, _, value ->
        builder.renderWith { createRecipeItem(context, value) }
            .onClick { click -> handleRecipeClick(context, click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Fabricações")
        config.layout(
            "         ",
            "  OOOOO  ",
            "  OOOOO  ",
            "    v    ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('v')
            .renderWith { backItem(categoryState.get(render)) }
            .onClick { click -> click.back() }
    }

    private fun backItem(category: CraftCategory) = itemStack(Material.ARROW) {
        meta<ItemMeta> {
            name("<yellow>Voltar")
            addLore { +"<dark_gray>${category.station.prettyName} • ${category.prettyName}" }
        }
    }

    private fun createRecipeItem(ctx: Context, value: ForgeRecipe): ItemStack {
        val unlocked = ForgeRecipeManager.isUnlocked(ctx.player, value)

        val base = if (unlocked) value.icon else ItemStack(Material.BARRIER)

        return itemStack(base) {
            val allRequirements = unlocked && CraftRequirements.hasAll(ctx.player, value)
            val colorName = if (allRequirements) "<#10FF47>" else "<#FF1E20>"

            meta {
                name(
                    if (!unlocked || value.icon.effectiveName().equals(value.icon.type.name))
                        Mini.mm.deserialize("$colorName${value.prettyName}")
                    else
                        value.icon.effectiveName()
                )

                addLore {
                    if (!unlocked) {
                        +""
                        +"<white>Requisitos de desbloqueio:"
                        +ForgeRecipeManager.requirementLines(ctx.player, value)
                    }

                    +""
                    +"<white>Custo de fabricação:"
                    value.requirements.forEach {
                        +"<dark_gray>▪ <gray>${it.amount}x ${it.prettyName}"
                    }

                    +""
                    when {
                        !unlocked -> +"<#FF1E20>Receita bloqueada."
                        allRequirements -> +"<#10FF47>Clique para fabricar!"
                        else -> +"<#FF1E20>Custo insuficiente."
                    }
                }
            }
        }
    }

    private fun handleRecipeClick(ctx: Context, click: SlotClickContext, forgeRecipe: ForgeRecipe) {
        val player = click.player

        if (!ForgeRecipeManager.isUnlocked(player, forgeRecipe)) {
            player.sendRichMessage("<red>Você ainda não desbloqueou essa receita!")
            return
        }

        if (!CraftRequirements.hasAll(player, forgeRecipe)) {
            player.sendRichMessage("<red>Você não possui todos os requisitos!")
            return
        }

        CraftRequirements.consume(player, forgeRecipe)
        player.inventory.addItem(forgeRecipe.result.clone())

        val customCraftEvent = CustomCraftEvent(player, forgeRecipe.id, forgeRecipe.result.amount)
        player.server.pluginManager.callEvent(customCraftEvent)

        player.sendRichMessage("<white>${forgeRecipe.prettyName} <green>fabricado com sucesso!")
        pagination.get(ctx).forceUpdate()
    }
}
