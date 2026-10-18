package one_block.system.islands.ext

import one_block.libs.instance.Instance
import org.bukkit.Bukkit

fun runNextTask(delay: Long = 1L, task: Runnable) {
    Bukkit.getScheduler().runTaskLater(Instance, task, delay)
}
