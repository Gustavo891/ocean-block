package one_block.system.spawners.models

import one_block.libs.coordinate.SimpleLocation
import java.util.UUID

data class PlacedSpawner(
    val uuid: UUID,
    val location: SimpleLocation,
    val typeId: String,
    val ownerUuid: UUID,
    var stackSize: Int = 1,
    var liveMobs: Int = 0,
    var enabled: Boolean = true,
) {
    fun type(): SpawnerType = SpawnerType.find(typeId)
}
