package ocean_block.system.drift.debris

import ocean_block.system.drift.config.DebrisType
import ocean_block.system.drift.lane.LanePoint
import org.bukkit.entity.ItemDisplay
import java.util.UUID

class Debris(
    val islandId: UUID,
    val type: DebrisType,
    val entity: ItemDisplay,
    var point: LanePoint,
    val float: FloatParams,
    var levelY: Double,
) {
    var ageTicks: Int = 0
    var depth: Int = 0
    var atSurface: Boolean = true
    var support: Support = Support.FLOAT
    var velocityY: Double = 0.0
    var restAgeTicks: Int = 0
    var stuckSinceMillis: Long? = null
    var fadeStartTick: Int? = null
    var pullTarget: UUID? = null
    var pullTicksLeft: Int = 0

    val isFree: Boolean get() = pullTarget == null
    val isFading: Boolean get() = fadeStartTick != null
    val isCollectable: Boolean get() = isFree && !isFading
}
