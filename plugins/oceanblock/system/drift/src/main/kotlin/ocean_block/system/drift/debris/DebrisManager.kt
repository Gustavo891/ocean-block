package ocean_block.system.drift.debris

import ocean_block.libs.instance.Instance
import ocean_block.system.drift.config.DriftConfig
import ocean_block.system.drift.config.DriftSettings
import ocean_block.system.drift.lane.DriftLane
import ocean_block.system.drift.loot.DebrisPicker
import ocean_block.system.drift.loot.DebrisPool
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

object DebrisManager {

    private val tracked = mutableListOf<Debris>()
    private val tasks = mutableListOf<BukkitTask>()

    private class ActiveLot(val island: Island, val world: World)

    fun start(config: DriftConfig, pool: DebrisPool) {
        stop()

        val settings = config.settings
        val scheduler = Bukkit.getScheduler()
        val spawnPeriod = settings.spawnIntervalTicks
        val movePeriod = DebrisRules.TICKS_PER_STEP.toLong()

        tasks += scheduler.runTaskTimer(Instance, Runnable { spawnTick(settings, pool) }, spawnPeriod, spawnPeriod)
        tasks += scheduler.runTaskTimer(Instance, Runnable { moveTick(settings) }, movePeriod, movePeriod)
    }

    fun stop() {
        tasks.forEach { it.cancel() }
        tasks.clear()
        tracked.forEach(::discard)
        tracked.clear()
    }

    fun nearestFree(location: Location, radius: Double): Debris? {
        val radiusSquared = radius * radius
        return tracked.asSequence()
            .filter { it.isFree && it.entity.isValid && it.entity.world == location.world }
            .map { it to it.entity.location.distanceSquared(location) }
            .filter { (_, distance) -> distance <= radiusSquared }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    fun beginPull(debris: Debris, player: Player) {
        if (!debris.isFree || debris !in tracked) return

        debris.pullTarget = player.uniqueId
        debris.pullTicksLeft = DebrisRules.PULL_TICKS
        debris.stuckSinceMillis = null
    }

    private fun activeLots(): Map<UUID, ActiveLot> =
        Bukkit.getOnlinePlayers()
            .mapNotNull { player -> IslandManager.getIslandAt(player.location)?.let { ActiveLot(it, player.world) } }
            .associateBy { it.island.uuid }

    private fun spawnTick(settings: DriftSettings, pool: DebrisPool) {
        activeLots().values
            .filter { lot -> tracked.count { it.islandId == lot.island.uuid } < settings.maxPerLot }
            .forEach { lot ->
                runCatching { spawnInto(lot, settings, pool) }
                    .onFailure { Instance.logger.warning("[deriva] falha ao criar detrito: ${it.message}") }
            }
    }

    private fun spawnInto(lot: ActiveLot, settings: DriftSettings, pool: DebrisPool) {
        val type = DebrisPicker.pick(pool.typesFor(lot.island), Random) ?: return
        val origin = lot.island.position
        val point = DriftLane.spawnPoint(
            origin.originX + DriftLane.BLOCK_CENTER,
            origin.originZ + DriftLane.BLOCK_CENTER,
            settings.direction,
            settings.laneLength,
            lateral(settings.laneWidth),
        )

        val params = FloatParams.roll(Random, settings.animation.speedVariation)
        val pose = DebrisPose.along(point, 0, params, settings, type.sink)
        if (!DebrisEntities.isLoaded(lot.world, floor(pose.point.x).toInt(), floor(pose.point.z).toInt())) return

        val entity = DebrisEntities.spawn(lot.world, pose, type)
        tracked += Debris(lot.island.uuid, type, entity, point, params)
    }

    private fun lateral(halfWidth: Double): Double =
        if (halfWidth <= 0.0) 0.0 else Random.nextDouble(-halfWidth, halfWidth)

    private fun moveTick(settings: DriftSettings) {
        val lots = activeLots()
        if (settings.streaks) lots.values.forEach { streaks(it, settings) }

        val tick = MotionTick(settings, lots.keys, System.currentTimeMillis())
        val iterator = tracked.iterator()

        while (iterator.hasNext()) {
            val debris = iterator.next()
            if (keep(debris, tick)) continue
            discard(debris)
            iterator.remove()
        }
    }

    private fun streaks(lot: ActiveLot, settings: DriftSettings) {
        val origin = lot.island.position
        runCatching {
            DebrisParticles.streaks(
                lot.world,
                origin.originX + DriftLane.BLOCK_CENTER,
                origin.originZ + DriftLane.BLOCK_CENTER,
                settings,
                Random,
            )
        }.onFailure { Instance.logger.warning("[deriva] falha ao desenhar riscos: ${it.message}") }
    }

    private fun keep(debris: Debris, tick: MotionTick): Boolean =
        runCatching { DebrisMotion.update(debris, tick) }
            .onFailure { Instance.logger.warning("[deriva] falha ao mover detrito: ${it.message}") }
            .getOrDefault(false)

    private fun discard(debris: Debris) {
        runCatching { debris.entity.remove() }
            .onFailure { Instance.logger.warning("[deriva] falha ao remover detrito: ${it.message}") }
    }
}
