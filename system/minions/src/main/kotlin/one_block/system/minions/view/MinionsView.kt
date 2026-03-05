package one_block.system.minions.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.minions.MinionsSystem
import one_block.system.minions.forge.ForgeRecipe
import one_block.system.minions.forge.ForgeRequirement
import org.bukkit.entity.Player

class MinionsView : View() {

    private val pagination = computedPaginationState(
        { MinionsSystem.recipes }
    ) { context, builder, index, value ->
        builder.renderWith { createMinionItem(context, value) }
            .onClick { click -> handleMinionClick(context, click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Fabricação de minion")
        config.layout(
            "         ",
            "  OOOOO  ",
            "         ",
        )
    }

    private fun createMinionItem(ctx: Context, value: ForgeRecipe) = itemStack(value.icon) {
        val allRequirements = hasAllRequirements(ctx.player, value)
        val colorName = if (allRequirements) "<#10FF47>" else "<#FF1E20>"

        meta {
            name("$colorName${value.prettyName}")

            addLore {
                +""
                +"<white>Custo de fabricação:"
                value.requirements.forEach {
                    +"<dark_gray>▪ <gray>${it.amount}x ${it.prettyName}"
                }

                +""
                if (allRequirements) {
                    +"<#10FF47>Clique para fabricar!"
                } else {
                    +"<#FF1E20>Custo insuficiente."
                }
            }
        }
    }

    private fun handleMinionClick(ctx: Context, click: SlotClickContext, forgeRecipe: ForgeRecipe) {
        val player = click.player

        if (!hasAllRequirements(player, forgeRecipe)) {
            player.sendMessage("<red>Você não possui todos os requisitos!")
            return
        }

        removeAllRequirements(player, forgeRecipe)
        player.inventory.addItem(forgeRecipe.result.clone())

        player.sendMessage("<green>Minion fabricado com sucesso!")
    }

    fun hasAllRequirements(player: Player, recipe: ForgeRecipe): Boolean {

        val inventoryItems = player.inventory.contents.filterNotNull().toMutableList()

        for (requirement in recipe.requirements) {

            var remaining = requirement.amount

            for (item in inventoryItems) {
                if (!item.isSimilar(requirement.item)) continue

                remaining -= item.amount
                if (remaining <= 0) break
            }

            if (remaining > 0) return false
        }

        return true
    }

    fun removeAllRequirements(player: Player, recipe: ForgeRecipe) {

        for (requirement in recipe.requirements) {

            var remaining = requirement.amount

            for (item in player.inventory.contents) {
                if (item == null) continue
                if (!item.isSimilar(requirement.item)) continue

                val remove = minOf(item.amount, remaining)

                item.amount -= remove
                remaining -= remove

                if (remaining <= 0) break
            }
        }
    }

    fun hasRequirement(player: Player, req: ForgeRequirement): Boolean {
        var amount = 0

        for (item in player.inventory.contents) {
            if (item == null) continue

            if (item.isSimilar(req.item)) {
                amount += item.amount
            }
        }

        return amount >= req.amount
    }

    fun removeRequirement(player: Player, req: ForgeRequirement) {
        var remaining = req.amount

        for (item in player.inventory.contents) {
            if (item == null) continue

            if (!item.isSimilar(req.item)) continue

            val remove = minOf(item.amount, remaining)

            item.amount -= remove
            remaining -= remove

            if (remaining <= 0) break
        }
    }
}
