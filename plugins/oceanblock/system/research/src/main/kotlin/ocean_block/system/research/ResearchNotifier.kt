package ocean_block.system.research

import ocean_block.system.islands.island.Island
import ocean_block.system.research.model.ResearchDefinition
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Sound

object ResearchNotifier {

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 100

    fun completed(island: Island, definition: ResearchDefinition) {
        val line = NotificationLine(
            "<green>Pesquisa concluída:</green> <white>${definition.name}</white>",
            definition.icon,
            DURATION_TICKS,
            "pesquisa:${definition.id}",
        )
        island.getIslandOnlinePlayers().forEach { player ->
            NotificationApi.push(player, REGION, line)
            player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f)
        }
    }
}
