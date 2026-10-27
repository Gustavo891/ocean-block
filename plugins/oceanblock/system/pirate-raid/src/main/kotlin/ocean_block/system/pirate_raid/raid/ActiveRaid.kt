package ocean_block.system.pirate_raid.raid

import ocean_block.system.islands.island.Island
import ocean_block.system.pirate_raid.rules.Band
import org.bukkit.Location
import org.bukkit.entity.Boat
import java.util.UUID

class RaidBoat(val boat: Boat) {
    var stillTicks = 0
    var landedAtTick: Long? = null
    var lastX = boat.location.x
    var lastZ = boat.location.z
}

class ActiveRaid(
    val island: Island,
    val level: Int,
    val band: Band,
    val center: Location,
    val shore: Location,
    val startTick: Long,
) {
    var wave = 0
    var emptyTicks = 0L
    val pirates = mutableSetOf<UUID>()
    val boats = mutableListOf<RaidBoat>()
    val viewers = mutableSetOf<UUID>()
    val strayTicks = mutableMapOf<UUID, Int>()

    val waves: Int get() = band.waves.size
}
