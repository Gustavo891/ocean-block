package ocean_block.system.islands.generator

import ocean_block.system.islands.island.Island
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class GeneratorFormedEvent(
    val island: Island
) : Event() {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
