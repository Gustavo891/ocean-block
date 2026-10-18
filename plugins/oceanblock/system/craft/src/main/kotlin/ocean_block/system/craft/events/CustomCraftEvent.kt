package ocean_block.system.craft.events

import org.bukkit.entity.Player
import org.bukkit.event.Event

class CustomCraftEvent (
    val player: Player,
    val id: String,
    val amount: Int
) : Event() {

     override fun getHandlers(): org.bukkit.event.HandlerList = HANDLERS

    companion object {
        @JvmStatic
        private val HANDLERS = org.bukkit.event.HandlerList()

        @JvmStatic
        fun getHandlerList(): org.bukkit.event.HandlerList = HANDLERS
    }
}
