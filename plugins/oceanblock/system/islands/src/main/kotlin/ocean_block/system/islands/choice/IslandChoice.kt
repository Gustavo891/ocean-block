package ocean_block.system.islands.choice

import ocean_block.system.islands.ext.runNextTask
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.type.IslandType
import one_block.textura.api.DialogApi
import org.bukkit.entity.Player

object IslandChoice {

    private const val RETRY_TICKS = 100L

    fun counts(): Map<IslandType, Int> = IslandManager.islandsById.values.groupingBy { it.type() }.eachCount()

    fun needsChoice(player: Player): Boolean = ChoiceRules.needsChoice(
        ownsIsland = IslandManager.getIslandByOwnerUUID(player.uniqueId) != null,
        memberSomewhere = IslandManager.getHomeIsland(player) != null,
    )

    fun begin(player: Player) {
        ChoiceCamera.enter(player)
        IslandTypeDialog.open(player)
    }

    fun confirm(player: Player, type: IslandType) {
        DialogApi.close(player)
        player.closeInventory()

        val home = IslandManager.getHomeIsland(player)
        if (home != null) {
            ChoiceCamera.leave(player)
            home.teleportTo(player)
            return
        }

        player.sendRichMessage("<green>Criando sua ilha...")
        val created = IslandManager.createIsland(player, type) { ready -> finish(player, ready) }
        if (created != null) return

        player.sendRichMessage("<red>Não foi possível criar sua ilha agora. Tente de novo em instantes.")
        runNextTask(RETRY_TICKS) {
            if (player.isOnline && needsChoice(player)) IslandTypeDialog.open(player)
        }
    }

    private fun finish(player: Player, island: Island) {
        if (!player.isOnline) return
        ChoiceCamera.leave(player)
        island.teleportTo(player)
        player.fallDistance = 0f
        player.sendRichMessage("<green>Sua ilha foi criada!")
    }
}
