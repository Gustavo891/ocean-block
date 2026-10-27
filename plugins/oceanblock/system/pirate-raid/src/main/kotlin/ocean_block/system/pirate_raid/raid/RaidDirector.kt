package ocean_block.system.pirate_raid.raid

import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.level.PlayerLevels
import ocean_block.system.missions.hud.MissionHud
import ocean_block.system.pirate_raid.config.RaidConfig
import ocean_block.system.pirate_raid.crew.PirateFactory
import ocean_block.system.pirate_raid.data.RaidDays
import ocean_block.system.pirate_raid.notify.RaidNotifier
import ocean_block.system.pirate_raid.rules.RaidPhase
import ocean_block.system.pirate_raid.rules.RaidRoll
import ocean_block.system.pirate_raid.rules.RaidState
import ocean_block.system.pirate_raid.rules.WaveTable
import ocean_block.system.pirate_raid.text.RaidHudText
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import kotlin.random.Random

object RaidDirector {

    private const val UPDATE_TICKS = 20L
    private const val STRAY_LIMIT = 10
    private const val TICKS_PER_SECOND = 20

    lateinit var config: RaidConfig
        private set

    private val raids = mutableMapOf<UUID, ActiveRaid>()
    private val random = Random.Default

    fun start(plugin: JavaPlugin, config: RaidConfig) {
        this.config = config
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { roll() }, config.intervalTicks, config.intervalTicks)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 1L, 1L)
    }

    fun raidOf(island: UUID): ActiveRaid? = raids[island]

    fun begin(island: Island, level: Int): Boolean {
        if (raids.containsKey(island.uuid)) return false
        val center = island.center.toBukkit()?.add(0.5, 0.0, 0.5) ?: return false
        val shore = island.spawn.toBukkit() ?: center.clone().add(0.0, 1.0, 0.0)
        val band = WaveTable.band(config.bands, level)
        val raid = ActiveRaid(island, level, band, center, shore, now())
        raids[island.uuid] = raid
        nextWave(raid, 1)
        members(raid).forEach(RaidNotifier::started)
        update(raid)
        return true
    }

    fun stop(island: UUID): Boolean {
        val raid = raids[island] ?: return false
        finish(raid, RaidPhase.Fled)
        return true
    }

    fun pirateDied(island: UUID, pirate: UUID) {
        val raid = raids[island] ?: return
        raid.pirates.remove(pirate)
        raid.strayTicks.remove(pirate)
    }

    fun levelOf(island: Island): Int =
        Bukkit.getOnlinePlayers().filter { inside(island, it) }.maxOfOrNull(PlayerLevels::levelOf) ?: 0

    fun shutdown() {
        raids.values.toList().forEach { clear(it) }
        raids.clear()
    }

    private fun roll() {
        val today = RaidDays.today()
        Bukkit.getOnlinePlayers()
            .mapNotNull { player -> IslandManager.getIslandAt(player.location)?.takeIf { inside(it, player) } }
            .distinctBy { it.uuid }
            .forEach { island ->
                val level = levelOf(island)
                val eligible = RaidRoll.eligible(raids.containsKey(island.uuid), RaidDays.lastDay(island.uuid), today, level, config.minLevel)
                if (eligible && RaidRoll.hits(random.nextDouble(), config.chance)) {
                    RaidDays.mark(island.uuid, today)
                    begin(island, level)
                }
            }
    }

    private fun tick() {
        if (raids.isEmpty()) return
        val now = now()
        raids.values.toList().forEach { raid ->
            BoatPilot.tick(raid, config, now)
            if ((now - raid.startTick) % UPDATE_TICKS == 0L) update(raid)
        }
    }

    private fun update(raid: ActiveRaid) {
        prune(raid)
        val members = members(raid)
        raid.emptyTicks = if (members.isEmpty()) raid.emptyTicks + UPDATE_TICKS else 0
        val elapsed = now() - raid.startTick
        val durationTicks = config.durationSeconds.toLong() * TICKS_PER_SECOND
        val phase = RaidState.next(
            raid.wave, raid.waves, raid.pirates.size, elapsed, durationTicks,
            raid.emptyTicks, config.emptySeconds.toLong() * TICKS_PER_SECOND,
        )
        when (phase) {
            is RaidPhase.Wave -> {
                if (phase.number > raid.wave) nextWave(raid, phase.number)
                refreshViewers(raid, members, ((durationTicks - elapsed) / TICKS_PER_SECOND).toInt())
            }
            RaidPhase.Won, RaidPhase.Fled, RaidPhase.Abandoned -> finish(raid, phase)
        }
    }

    private fun nextWave(raid: ActiveRaid, wave: Int) {
        raid.wave = wave
        WaveSpawner.spawn(raid, wave, config, random)
        members(raid).forEach(RaidNotifier::horn)
    }

    private fun prune(raid: ActiveRaid) {
        raid.pirates.toList().forEach { id ->
            val pirate = Bukkit.getEntity(id) as? LivingEntity
            if (pirate == null || !pirate.isValid || pirate.isDead) {
                raid.pirates.remove(id)
                raid.strayTicks.remove(id)
                pirate?.let(PirateFactory::forget)
                return@forEach
            }
            leash(raid, pirate)
        }
    }

    private fun leash(raid: ActiveRaid, pirate: LivingEntity) {
        if (pirate.isInsideVehicle) return
        val far = pirate.world != raid.center.world || pirate.location.distance(raid.center) > config.leashDistance
        val strays = if (far) (raid.strayTicks[pirate.uniqueId] ?: 0) + 1 else 0
        raid.strayTicks[pirate.uniqueId] = strays
        if (strays >= STRAY_LIMIT) {
            pirate.teleport(raid.shore)
            raid.strayTicks[pirate.uniqueId] = 0
        }
        val mob = pirate as? Mob ?: return
        if (mob.target == null) nearest(raid, pirate.location)?.let { mob.target = it }
    }

    private fun refreshViewers(raid: ActiveRaid, members: List<Player>, secondsLeft: Int) {
        val text = RaidHudText.line(raid.wave, raid.waves, raid.pirates.size, secondsLeft)
        val present = members.map { it.uniqueId }.toSet()
        raid.viewers.toList().filter { it !in present }.forEach { id ->
            raid.viewers.remove(id)
            Bukkit.getPlayer(id)?.let(::release)
        }
        members.forEach { player ->
            if (raid.viewers.add(player.uniqueId)) MissionHud.suspend(player)
            RaidNotifier.hud(player, text)
        }
    }

    private fun finish(raid: ActiveRaid, phase: RaidPhase) {
        val members = members(raid)
        when (phase) {
            RaidPhase.Won -> members.forEach { reward(raid, it) }
            RaidPhase.Fled -> members.forEach(RaidNotifier::fled)
            else -> Unit
        }
        clear(raid)
        raids.remove(raid.island.uuid)
    }

    private fun reward(raid: ActiveRaid, player: Player) {
        val coins = RaidRewards.pay(player, raid.level, config)
        RaidNotifier.won(player, coins)
    }

    private fun clear(raid: ActiveRaid) {
        raid.pirates.mapNotNull(Bukkit::getEntity).forEach { pirate ->
            pirate.world.spawnParticle(Particle.LARGE_SMOKE, pirate.location.add(0.0, 1.0, 0.0), 12, 0.3, 0.5, 0.3, 0.02)
            PirateFactory.forget(pirate)
            pirate.remove()
        }
        raid.pirates.clear()
        BoatPilot.removeAll(raid)
        raid.viewers.mapNotNull(Bukkit::getPlayer).forEach(::release)
        raid.viewers.clear()
    }

    private fun release(player: Player) {
        RaidNotifier.hideHud(player)
        MissionHud.resume(player)
    }

    private fun members(raid: ActiveRaid): List<Player> =
        Bukkit.getOnlinePlayers().filter { inside(raid.island, it) }

    private fun inside(island: Island, player: Player): Boolean =
        CollectorRule.canCollect(island, player) && IslandManager.getIslandAt(player.location)?.uuid == island.uuid

    private fun nearest(raid: ActiveRaid, location: Location): Player? =
        members(raid).filter { it.world == location.world }.minByOrNull { it.location.distanceSquared(location) }

    private fun now(): Long = Bukkit.getCurrentTick().toLong()
}
