package ocean_block.system.craft.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import ocean_block.system.craft.screen.RecipesScreen
import org.bukkit.entity.Player

@CommandAlias("receitas|craft|receita")
class ReceitasCommand(private val screen: RecipesScreen) : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        screen.open(player)
    }
}
