package one_block.system.items.tasks

import one_block.system.items.SpecialItemSystem
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class BeamTask : BukkitRunnable() {

    override fun run() {
        val iterator = SellTask().beamMap.iterator()
        while (iterator.hasNext()) {
            val beam = iterator.next()
            if(beam.tick()) {
                iterator.remove()
            }
        }
    }

    fun start() {
        runTaskTimer(SpecialItemSystem.plugin, 0L, 1L)
    }
}