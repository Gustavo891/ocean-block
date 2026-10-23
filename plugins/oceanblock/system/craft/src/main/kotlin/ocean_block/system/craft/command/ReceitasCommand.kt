package ocean_block.system.craft.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.craft.menu.RecipesMenu
import ocean_block.system.craft.recipe.Recipes
import org.bukkit.entity.Player

@CommandAlias("receitas|craft|receita")
class ReceitasCommand(private val viewFrame: ViewFrame) : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        if (Recipes.byCategory().isEmpty()) {
            player.sendRichMessage("<red>Nenhuma receita disponível.")
            return
        }
        viewFrame.open(RecipesMenu::class.java, player)
    }
}
