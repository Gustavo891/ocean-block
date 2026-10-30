package ocean_block.system.islands.account

import java.util.UUID

class PendingResets {

    private val uuids = mutableSetOf<UUID>()

    fun load(stored: Collection<UUID>) {
        uuids += stored
    }

    fun add(uuid: UUID): Boolean = uuids.add(uuid)

    fun consume(uuid: UUID): Boolean = uuids.remove(uuid)

    fun contains(uuid: UUID): Boolean = uuid in uuids
}
