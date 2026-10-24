package ocean_block.system.machines.energy

import ocean_block.libs.instance.Instance
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.preferences.Preference
import ocean_block.system.preferences.Preferences
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import kotlin.random.Random

object EnergyBeams {

    const val REFRESH_TICKS = 2L

    private const val THICKNESS = 0.06f
    private const val JITTER = 0.18
    private const val LIFE_TICKS = 25
    private const val VIEW_DISTANCE = 32.0
    private const val FULL_BRIGHT = 15

    private val GLOW = Color.fromRGB(90, 220, 255)
    private val CORE = Material.LIGHT_BLUE_CONCRETE.createBlockData()
    private val AXIS = Vector3f(0f, 0f, 1f)

    private class Beam(val origin: Location, val target: BeamPoint, val segments: List<BlockDisplay>, var expires: Int) {
        val isValid: Boolean get() = segments.all { it.isValid }
        fun remove() = segments.forEach { it.remove() }
    }

    private val beams = mutableMapOf<Pair<UUID, UUID>, Beam>()

    fun pulse(source: PlacedBlock, consumer: PlacedBlock) {
        val origin = source.location.toBukkit()?.add(0.5, 0.5, 0.5) ?: return
        val end = consumer.location.toBukkit()?.add(0.5, 0.5, 0.5) ?: return
        if (origin.world != end.world) return
        val key = source.uuid to consumer.uuid
        val beam = beams[key]?.takeIf { it.isValid } ?: create(origin, end).also { beams.put(key, it)?.remove() }
        beam.expires = Bukkit.getCurrentTick() + LIFE_TICKS
    }

    fun refresh() {
        val now = Bukkit.getCurrentTick()
        beams.entries.removeIf { (_, beam) ->
            val gone = now > beam.expires || !beam.isValid
            if (gone) beam.remove() else {
                reshape(beam)
                updateViewers(beam)
            }
            gone
        }
    }

    fun removeAll() {
        beams.values.forEach { it.remove() }
        beams.clear()
    }

    private fun create(origin: Location, end: Location): Beam {
        val target = BeamPoint(end.x - origin.x, end.y - origin.y, end.z - origin.z)
        val count = BeamPath.segmentsFor(BeamPoint(0.0, 0.0, 0.0).distanceTo(target))
        val segments = List(count) {
            origin.world.spawn(origin, BlockDisplay::class.java) { display ->
                display.isPersistent = false
                display.isVisibleByDefault = false
                display.block = CORE
                display.brightness = Display.Brightness(FULL_BRIGHT, FULL_BRIGHT)
                display.isGlowing = true
                display.glowColorOverride = GLOW
            }
        }
        return Beam(origin, target, segments, Bukkit.getCurrentTick() + LIFE_TICKS).also {
            reshape(it)
            updateViewers(it)
        }
    }

    private fun reshape(beam: Beam) {
        val points = BeamPath.points(BeamPoint(0.0, 0.0, 0.0), beam.target, beam.segments.size, JITTER, Random.Default)
        beam.segments.forEachIndexed { index, display ->
            val start = points[index]
            val end = points[index + 1]
            val direction = Vector3f((end.x - start.x).toFloat(), (end.y - start.y).toFloat(), (end.z - start.z).toFloat())
            val length = direction.length()
            val rotation = Quaternionf().rotationTo(AXIS, direction.normalize())
            val translation = Vector3f(start.x.toFloat(), start.y.toFloat(), start.z.toFloat())
                .add(Vector3f(-THICKNESS / 2, -THICKNESS / 2, 0f).rotate(rotation))
            display.interpolationDelay = 0
            display.interpolationDuration = REFRESH_TICKS.toInt()
            display.transformation = Transformation(translation, rotation, Vector3f(THICKNESS, THICKNESS, length), Quaternionf())
        }
    }

    private fun updateViewers(beam: Beam) {
        val center = beam.origin.clone().add(beam.target.x / 2, beam.target.y / 2, beam.target.z / 2)
        beam.origin.world.players.forEach { player ->
            val sees = player.location.distanceSquared(center) <= VIEW_DISTANCE * VIEW_DISTANCE &&
                Preferences.isEnabled(player, Preference.ENERGY_TRAIL)
            beam.segments.forEach { display ->
                if (sees) player.showEntity(Instance, display) else player.hideEntity(Instance, display)
            }
        }
    }
}
