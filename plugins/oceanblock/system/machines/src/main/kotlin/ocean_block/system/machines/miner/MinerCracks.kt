package ocean_block.system.machines.miner

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.block.Block
import java.util.UUID

object MinerCracks {

    const val REFRESH_TICKS = 2L
    private const val VIEW_DISTANCE = 32.0
    private const val PARTICLES = 4
    private const val HIT_VOLUME = 0.35f
    private const val HIT_PITCH = 0.8f
    private const val STALE_TICKS = MinerCrackRules.CYCLE_TICKS * 3

    private class Crack(val block: Location, val source: Int, var paid: Int, var gain: Int, var paidAt: Int, var stage: Int = -1)

    private val cracks = mutableMapOf<UUID, Crack>()

    fun update(miner: UUID, block: Block, progress: Int, gain: Int) {
        val location = block.location
        val current = cracks[miner]
        if (current != null && current.block != location) clear(miner)
        val crack = cracks.getOrPut(miner) { Crack(location, sourceId(miner), progress, gain, Bukkit.getCurrentTick()) }
        crack.paid = progress
        crack.gain = gain
        crack.paidAt = Bukkit.getCurrentTick()
    }

    fun clear(miner: UUID) {
        val crack = cracks.remove(miner) ?: return
        send(crack.block, 0f, crack.source)
    }

    fun clearAll() {
        cracks.keys.toList().forEach(::clear)
    }

    fun refresh() {
        val now = Bukkit.getCurrentTick()
        cracks.filterValues { now - it.paidAt > STALE_TICKS }.keys.forEach(::clear)
        cracks.values.forEach { crack ->
            val stage = MinerCrackRules.stage(MinerCrackRules.progress(crack.paid, crack.gain, now - crack.paidAt))
            if (stage == crack.stage) return@forEach
            crack.stage = stage
            send(crack.block, MinerCrackRules.damage(stage), crack.source)
            hit(crack.block.block)
        }
    }

    private fun hit(block: Block) {
        if (block.type.isAir) return
        val data = block.blockData
        val top = block.location.add(0.5, 0.95, 0.5)
        block.world.spawnParticle(Particle.BLOCK, top, PARTICLES, 0.25, 0.05, 0.25, data)
        block.world.playSound(top, data.soundGroup.hitSound, HIT_VOLUME, HIT_PITCH)
    }

    private fun send(location: Location, damage: Float, source: Int) {
        val world = location.world ?: return
        world.players.filter { it.location.distanceSquared(location) <= VIEW_DISTANCE * VIEW_DISTANCE }
            .forEach { it.sendBlockDamage(location, damage, source) }
    }

    private fun sourceId(miner: UUID): Int = -1 - (miner.hashCode() and Int.MAX_VALUE)
}
