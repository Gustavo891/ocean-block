package ocean_block.system.drift.collect

import ocean_block.system.islands.island.Island
import org.bukkit.entity.Player
import java.util.UUID

object CollectorRule {

    fun canCollect(
        ownerUuid: UUID,
        members: Set<String>,
        trusted: Set<String>,
        playerUuid: UUID,
        playerName: String,
    ): Boolean {
        if (playerUuid == ownerUuid) return true
        val name = playerName.lowercase()
        return name in members || name in trusted
    }

    fun canCollect(island: Island, player: Player): Boolean =
        canCollect(
            island.ownerUuid,
            island.members.keys,
            island.trustedNames,
            player.uniqueId,
            player.name,
        )
}
