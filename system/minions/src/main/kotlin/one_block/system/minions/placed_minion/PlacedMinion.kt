package one_block.system.minions.placed_minion

import one_block.libs.coordinate.SimpleLocation
import one_block.system.minions.enums.MinionType
import java.util.UUID

data class PlacedMinion(
    val uuid: UUID,
    val location: SimpleLocation,
    val type: MinionType,
)