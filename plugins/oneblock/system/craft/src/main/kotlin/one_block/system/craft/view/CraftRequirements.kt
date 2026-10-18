package one_block.system.craft.view

import one_block.system.craft.forge.ForgeRecipe
import org.bukkit.entity.Player

object CraftRequirements {

    fun hasAll(player: Player, recipe: ForgeRecipe): Boolean {
        val itens = player.inventory.contents.filterNotNull()

        for (requirement in recipe.requirements) {
            var restante = requirement.amount

            for (item in itens) {
                if (!requirement.matches(item)) continue

                restante -= item.amount
                if (restante <= 0) break
            }

            if (restante > 0) return false
        }

        return true
    }

    fun consume(player: Player, recipe: ForgeRecipe) {
        for (requirement in recipe.requirements) {
            var restante = requirement.amount

            for (item in player.inventory.contents) {
                if (item == null) continue
                if (!requirement.matches(item)) continue

                val remover = minOf(item.amount, restante)

                item.amount -= remover
                restante -= remover

                if (restante <= 0) break
            }
        }
    }
}
