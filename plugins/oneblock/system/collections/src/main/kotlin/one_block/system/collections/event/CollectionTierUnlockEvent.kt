package one_block.system.collections.event

import one_block.system.collections.collection.CollectionEntry
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class CollectionTierUnlockEvent(
    val player: Player,
    val entry: CollectionEntry,
    val tier: Int,
) : Event() {

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {

        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
