package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DebrisType
import ocean_block.system.drift.lane.LanePoint
import org.bukkit.entity.ItemDisplay
import java.util.UUID

class Debris(val islandId: UUID, val type: DebrisType, val entity: ItemDisplay, var point: LanePoint) {
    var stuckSinceMillis: Long? = null
    var pullTarget: UUID? = null
    var pullTicksLeft: Int = 0

    val isFree: Boolean get() = pullTarget == null
}
