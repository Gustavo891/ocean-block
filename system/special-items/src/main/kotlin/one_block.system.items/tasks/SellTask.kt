package one_block.system.items.tasks

import one_block.system.items.SpecialItemSystem
import one_block.system.items.sell.ParticleBeam
import one_block.system.items.sell.SellBeaconManager
import org.bukkit.Particle
import org.bukkit.entity.Item
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.CopyOnWriteArrayList

class SellTask : BukkitRunnable() {

    private val taskInterval = 60L
    val beamMap: CopyOnWriteArrayList<ParticleBeam> = CopyOnWriteArrayList()

    fun start() {
        BeamTask().start()
        runTaskTimer(SpecialItemSystem.plugin, 0L, taskInterval)
    }

    override fun run() {

        SellBeaconManager.hopperbyUUID.forEach { hop ->
            val hopper = hop.value
//            val island = IslandManager.getIslandById(hopper.islandUUID) ?: return@forEach
//            val player = plugin.server.getPlayer(hopper.ownerUUID) ?: return@forEach
            val loc = hopper.coordinate.toBukkit() ?: return@forEach
            val dropItems = loc.getNearbyEntitiesByType(Item::class.java, 10.0)
            val drop = dropItems.random() ?: return@forEach

            val element = ParticleBeam(loc, loc.distance(drop.location), Particle.COMPOSTER)
            beamMap.add(element)

        }

    }
}