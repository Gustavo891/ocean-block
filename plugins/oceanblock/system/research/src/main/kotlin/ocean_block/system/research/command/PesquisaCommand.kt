package ocean_block.system.research.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.screen.ResearchScreen
import org.bukkit.entity.Player

@CommandAlias("pesquisa|pesquisas")
class PesquisaCommand : BaseCommand() {

    @Default
    fun onDefault(player: Player) {
        val island = IslandManager.getIslandByPlayer(player.uniqueId) ?: run {
            player.sendRichMessage("<red>Você precisa de uma ilha para pesquisar. Use <white>/ilha</white>.")
            return
        }
        ResearchScreen.open(player, island.uuid)
    }
}
