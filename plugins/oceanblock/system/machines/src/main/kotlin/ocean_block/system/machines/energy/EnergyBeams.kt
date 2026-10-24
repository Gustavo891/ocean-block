package ocean_block.system.machines.energy

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.preferences.Preference
import ocean_block.system.preferences.Preferences
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.util.UUID

object EnergyBeams {

    const val REFRESH_TICKS = 2L

    private const val SPACING = 0.15
    private const val LIFE_TICKS = 25
    private const val VIEW_DISTANCE = 32.0
    private const val LIFT = 0.08

    private val COLOR = Particle.DustOptions(Color.fromRGB(80, 210, 255), 0.6f)

    private class Beam(val origin: Location, val target: BeamPoint, var expires: Int)

    private val beams = mutableMapOf<Pair<UUID, UUID>, Beam>()

    fun pulse(source: PlacedBlock, consumer: PlacedBlock) {
        val origin = topOf(source) ?: return
        val end = topOf(consumer) ?: return
        if (origin.world != end.world) return
        val target = BeamPoint(end.x - origin.x, end.y - origin.y, end.z - origin.z)
        beams[source.uuid to consumer.uuid] = Beam(origin, target, Bukkit.getCurrentTick() + LIFE_TICKS)
    }

    fun refresh() {
        val now = Bukkit.getCurrentTick()
        beams.entries.removeIf { (_, beam) -> now > beam.expires }
        beams.values.forEach(::draw)
    }

    fun removeAll() = beams.clear()

    private fun draw(beam: Beam) {
        val viewers = viewersOf(beam)
        if (viewers.isEmpty()) return
        val points = BeamPath.line(BeamPoint(0.0, 0.0, 0.0), beam.target, SPACING)
        viewers.forEach { player ->
            points.forEach { point ->
                player.spawnParticle(Particle.DUST, beam.origin.clone().add(point.x, point.y, point.z), 0, 0.0, 0.0, 0.0, 0.0, COLOR)
            }
        }
    }

    private fun topOf(placed: PlacedBlock): Location? {
        val block = placed.location.toBukkit()?.block ?: return null
        val top = block.boundingBox.takeIf { it.volume > 0 }?.maxY ?: (block.y + 1.0)
        return Location(block.world, block.x + 0.5, top + LIFT, block.z + 0.5)
    }

    private fun viewersOf(beam: Beam): List<Player> {
        val center = beam.origin.clone().add(beam.target.x / 2, beam.target.y / 2, beam.target.z / 2)
        return beam.origin.world.players.filter {
            it.location.distanceSquared(center) <= VIEW_DISTANCE * VIEW_DISTANCE && Preferences.isEnabled(it, Preference.ENERGY_TRAIL)
        }
    }
}
