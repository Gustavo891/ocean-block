package one_block.system.special_items.tasks

import one_block.system.special_items.SpecialItemSystem
import one_block.system.special_items.sell.ParticleBeam
import one_block.system.special_items.sell.SellBeaconManager
import one_block.system.sell.manager.SellManager
import org.bukkit.Particle
import org.bukkit.entity.Item
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.CopyOnWriteArrayList

class SellTask : BukkitRunnable() {

    private val taskInterval = 60L
    val beamMap: CopyOnWriteArrayList<ParticleBeam> = CopyOnWriteArrayList()

    init {
        runTaskTimer(SpecialItemSystem.plugin, 0L, taskInterval)
    }

    override fun run() {
        SellBeaconManager.beaconbyUUID.forEach { hop ->
            val beacon = hop.value
            val loc = beacon.coordinate.toBukkit() ?: return@forEach

            val drop = loc.getNearbyEntitiesByType(Item::class.java, 10.0)
                .filter { it.isOnGround && SellManager.getPrice(it.itemStack.type) != 0.0 }
                .randomOrNull() ?: return@forEach

            val startLoc = beacon.display?.location ?: loc.clone().add(0.5, 2.0, 0.5)

            val element = ParticleBeam(startLoc, drop.location, Particle.COMPOSTER) {
                if (!drop.isValid || drop.isDead || !drop.isOnGround) return@ParticleBeam

                val price = SellManager.getPrice(drop.itemStack.type)
                if (price == 0.0) return@ParticleBeam

                val total = price * drop.itemStack.amount
                drop.remove()

                beacon.coinsAmount += total
                SellBeaconManager.markDirty(beacon.uuid)
            }

            beamMap.add(element)
        }
    }
}
