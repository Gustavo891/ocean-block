package ocean_block.system.pirate_raid.raid

import ocean_block.system.pirate_raid.config.RaidConfig
import ocean_block.system.pirate_raid.crew.PirateFactory
import ocean_block.system.pirate_raid.crew.PirateKeys
import ocean_block.system.pirate_raid.crew.PirateKind
import ocean_block.system.pirate_raid.rules.BoatPath
import ocean_block.system.pirate_raid.rules.Vec
import ocean_block.system.pirate_raid.rules.WaveTable
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.boat.DarkOakBoat
import org.bukkit.persistence.PersistentDataType
import kotlin.math.PI
import kotlin.random.Random

object WaveSpawner {

    private const val ATTEMPTS = 12
    private const val RADIUS_STEP = 4.0
    private const val SCAN_TOP = 80
    private const val SCAN_BOTTOM = 40
    private const val SEATS = 2
    private const val BORDER_MARGIN = 3.0

    fun spawn(raid: ActiveRaid, wave: Int, config: RaidConfig, random: Random) {
        val crew = WaveTable.crew(raid.band, wave, random)
        val regular = buildList {
            repeat(crew.brutes) { add(PirateKind.BRUTE) }
            repeat(crew.pillagers) { add(PirateKind.PILLAGER) }
        }.shuffled(random)
        val groups = regular.chunked(SEATS) + if (crew.superPirate) listOf(listOf(PirateKind.SUPER)) else emptyList()
        groups.forEach { group -> launch(raid, group, config, random) }
    }

    private fun launch(raid: ActiveRaid, group: List<PirateKind>, config: RaidConfig, random: Random) {
        val water = waterPoint(raid, config, random)
        if (water == null) {
            group.forEach { kind -> board(raid, kind, raid.shore, config) }
            return
        }
        val boat = water.world.spawn(water, DarkOakBoat::class.java) { boat ->
            boat.isPersistent = false
            boat.persistentDataContainer.set(PirateKeys.BOAT, PersistentDataType.BYTE, 1)
        }
        water.world.spawnParticle(Particle.SPLASH, water, 40, 0.8, 0.2, 0.8)
        group.forEach { kind -> boat.addPassenger(board(raid, kind, water, config)) }
        raid.boats += RaidBoat(boat)
    }

    private fun board(raid: ActiveRaid, kind: PirateKind, location: Location, config: RaidConfig) =
        PirateFactory.spawn(location, kind, raid.island.uuid, config.superPirate.health).also { raid.pirates += it.uniqueId }

    private fun waterPoint(raid: ActiveRaid, config: RaidConfig, random: Random): Location? {
        val center = raid.center
        var radius = minOf(config.boatDistance, raid.island.borderSize() / 2.0 - BORDER_MARGIN)
        while (radius >= config.boatMinDistance) {
            repeat(ATTEMPTS) {
                val point = BoatPath.spawnPoint(Vec(center.x, center.z), radius, random.nextDouble(0.0, 2 * PI))
                surface(center.world, point)?.let { return it }
            }
            radius -= RADIUS_STEP
        }
        return null
    }

    private fun surface(world: World, point: Vec): Location? {
        val x = kotlin.math.floor(point.x).toInt()
        val z = kotlin.math.floor(point.z).toInt()
        if (!world.isChunkLoaded(x shr 4, z shr 4)) return null
        for (y in SCAN_TOP downTo SCAN_BOTTOM) {
            val type = world.getBlockAt(x, y, z).type
            if (type == Material.AIR || type == Material.CAVE_AIR) continue
            if (type != Material.WATER) return null
            return Location(world, x + 0.5, y + 1.0, z + 0.5)
        }
        return null
    }
}
