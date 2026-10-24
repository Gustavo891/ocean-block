package ocean_block.system.machines.energy

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.preferences.Preference
import ocean_block.system.preferences.Preferences
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object EnergyEffects {

    private const val TRAIL_VIEW_DISTANCE = 24.0
    private const val TRAIL_STEP = 0.4
    private const val TRAIL_CYCLE = 4
    private const val RADIUS_VIEW_DISTANCE = 48.0
    private const val RADIUS_POINTS = 72

    private val TRAIL_DUST = Particle.DustOptions(Color.fromRGB(255, 208, 64), 0.7f)
    private val RADIUS_DUST = Particle.DustOptions(Color.fromRGB(80, 220, 240), 1.0f)

    private val radiusViewers = ConcurrentHashMap<UUID, UUID>()

    fun trail(source: PlacedBlock, consumer: PlacedBlock) {
        val from = source.location.toBukkit()?.add(0.5, 0.5, 0.5) ?: return
        val to = consumer.location.toBukkit()?.add(0.5, 0.5, 0.5) ?: return
        if (from.world != to.world) return
        val viewers = to.world.players.filter {
            it.location.distanceSquared(to) <= TRAIL_VIEW_DISTANCE * TRAIL_VIEW_DISTANCE &&
                Preferences.isEnabled(it, Preference.ENERGY_TRAIL)
        }
        if (viewers.isEmpty()) return
        val direction = to.toVector().subtract(from.toVector())
        val length = direction.length()
        if (length == 0.0) return
        direction.normalize()
        val phase = EnergyShapes.phase(Bukkit.getCurrentTick(), TRAIL_CYCLE, TRAIL_STEP)
        EnergyShapes.trail(length, TRAIL_STEP, phase).forEach { distance ->
            val point = from.clone().add(direction.clone().multiply(distance))
            viewers.forEach { it.spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0, 0.0, TRAIL_DUST) }
        }
    }

    fun isShowingRadius(player: Player, source: PlacedBlock): Boolean = radiusViewers[player.uniqueId] == source.uuid

    fun toggleRadius(player: Player, source: PlacedBlock): Boolean {
        if (isShowingRadius(player, source)) {
            radiusViewers.remove(player.uniqueId)
            return false
        }
        radiusViewers[player.uniqueId] = source.uuid
        return true
    }

    fun forget(player: Player) {
        radiusViewers.remove(player.uniqueId)
    }

    fun drawRadii() {
        radiusViewers.entries.removeIf { (playerId, sourceId) ->
            val player = Bukkit.getPlayer(playerId) ?: return@removeIf true
            val source = PlacedBlockManager.get(sourceId)?.takeIf { PlacedBlockManager.isRegistered(it) } ?: return@removeIf true
            val center = source.location.toBukkit()?.add(0.5, 0.5, 0.5) ?: return@removeIf true
            if (center.world != player.world || player.location.distanceSquared(center) > RADIUS_VIEW_DISTANCE * RADIUS_VIEW_DISTANCE) {
                return@removeIf true
            }
            drawCircle(player, center)
            false
        }
    }

    private fun drawCircle(player: Player, center: Location) {
        EnergyShapes.circle(EnergyRules.RADIUS, RADIUS_POINTS).forEach { (x, z) ->
            player.spawnParticle(Particle.DUST, center.clone().add(x, 0.0, z), 1, 0.0, 0.0, 0.0, 0.0, RADIUS_DUST)
        }
    }
}
