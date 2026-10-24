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
import kotlin.random.Random

object EnergyBeams {

    const val REFRESH_TICKS = 2L

    private const val JITTER = 0.35
    private const val SPACING = 0.12
    private const val LIFE_TICKS = 25
    private const val VIEW_DISTANCE = 32.0
    private const val LIFT = 0.08

    private val CORE = Particle.DustOptions(Color.fromRGB(225, 250, 255), 0.45f)
    private val GLOW = Particle.DustOptions(Color.fromRGB(70, 200, 255), 0.8f)

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
        val start = BeamPoint(0.0, 0.0, 0.0)
        val nodes = BeamPath.points(start, beam.target, BeamPath.segmentsFor(start.distanceTo(beam.target)), JITTER, Random.Default)
        val samples = BeamPath.sample(nodes, SPACING)
        viewers.forEach { player ->
            samples.forEachIndexed { index, point ->
                val at = beam.origin.clone().add(point.x, point.y, point.z)
                player.spawnParticle(Particle.DUST, at, 1, 0.0, 0.0, 0.0, 0.0, CORE)
                if (index % 2 == 0) player.spawnParticle(Particle.DUST, at, 1, 0.02, 0.02, 0.02, 0.0, GLOW)
            }
            nodes.drop(1).dropLast(1).forEach { node ->
                if (Random.nextInt(3) == 0) player.spawnParticle(Particle.ELECTRIC_SPARK, beam.origin.clone().add(node.x, node.y, node.z), 1, 0.0, 0.0, 0.0, 0.0)
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
