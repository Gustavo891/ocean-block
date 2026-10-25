package ocean_block.system.islands.island

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class IslandUpgradeEvent(
    val island: Island,
    val upgrade: IslandUpgrade,
    val level: Int
) : Event() {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
