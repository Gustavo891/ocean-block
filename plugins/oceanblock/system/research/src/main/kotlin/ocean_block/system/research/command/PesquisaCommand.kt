package ocean_block.system.research.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.ResearchTimer
import ocean_block.system.research.menu.ResearchMenu
import org.bukkit.entity.Player

@CommandAlias("pesquisa|pesquisas")
class PesquisaCommand(private val viewFrame: ViewFrame) : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        val island = IslandManager.getIslandByPlayer(player.uniqueId) ?: run {
            player.sendRichMessage("<red>Você precisa de uma ilha para pesquisar. Use <white>/ilha</white>.")
            return
        }
        ResearchTimer.completeDue(island.uuid)
        viewFrame.open(ResearchMenu::class.java, player, island.uuid)
    }
}
