package one_block.system.special_items.tasks

import one_block.system.special_items.SpecialItemSystem
import org.bukkit.scheduler.BukkitRunnable

class BeamTask(private val sellTask: SellTask) : BukkitRunnable() {

    override fun run() {
        sellTask.beamMap.removeIf { beam ->
            !beam.tick()
        }
    }

    init {
        runTaskTimer(SpecialItemSystem.plugin, 0L, 1L)
    }
}
