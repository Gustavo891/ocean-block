package ocean_block.system.islands.member

import java.util.UUID

data class IslandInvite(
    val islandUuid: UUID,
    val ownerName: String,
    val expiresAt: Long,
)

class IslandInvites(private val ttlMillis: Long) {

    private val byTarget = mutableMapOf<UUID, MutableMap<UUID, IslandInvite>>()

    fun add(target: UUID, islandUuid: UUID, ownerName: String, now: Long) {
        byTarget.getOrPut(target) { mutableMapOf() }[islandUuid] = IslandInvite(islandUuid, ownerName, now + ttlMillis)
    }

    fun pending(target: UUID, now: Long): List<IslandInvite> {
        val invites = byTarget[target] ?: return emptyList()
        invites.values.removeIf { it.expiresAt < now }
        if (invites.isEmpty()) byTarget.remove(target)
        return invites.values.toList()
    }

    fun take(target: UUID, ownerName: String?, now: Long): IslandInvite? {
        val alive = pending(target, now)
        val chosen = if (ownerName == null) {
            alive.singleOrNull()
        } else {
            alive.firstOrNull { it.ownerName.equals(ownerName, ignoreCase = true) }
        } ?: return null
        byTarget[target]?.remove(chosen.islandUuid)
        return chosen
    }

    fun forget(target: UUID) {
        byTarget.remove(target)
    }
}
