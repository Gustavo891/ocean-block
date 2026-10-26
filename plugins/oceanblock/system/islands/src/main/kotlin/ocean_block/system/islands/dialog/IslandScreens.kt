package ocean_block.system.islands.dialog

import ocean_block.libs.instance.Instance
import ocean_block.system.islands.island.Island
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBindsConfig
import one_block.textura.api.DialogOpenResult
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object IslandScreens {

    fun show(player: Player, screen: String, fallback: (Player) -> Unit, binds: DialogBindsConfig) {
        val result = DialogApi.openResult(player, screen, binds)
        if (result == DialogOpenResult.MissingScreen || result == DialogOpenResult.Failed) fallback(player)
    }

    fun later(player: Player, action: (Player) -> Unit) {
        Bukkit.getScheduler().runTask(Instance, Runnable { if (player.isOnline) action(player) })
    }

    fun isOwner(island: Island, player: Player): Boolean = island.ownerUuid == player.uniqueId
}
