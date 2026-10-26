package one_block.textura.api

import one_block.textura.dialog.node.UiOutcome
import org.bukkit.entity.Player

class DialogClick internal constructor(val player: Player, private val inputs: Map<String, String>) {

    internal constructor(player: Player) : this(player, emptyMap())

    fun input(key: String): String? = inputs[key]


    internal var outcome: UiOutcome = UiOutcome.Refresh
        private set

    fun close() {
        outcome = UiOutcome.Close
    }

    fun open(screenId: String) {
        open(screenId, emptyMap()) {}
    }

    fun open(screenId: String, binds: DialogBindsConfig) {
        open(screenId, emptyMap(), binds)
    }

    fun open(screenId: String, values: Map<String, String>, binds: DialogBindsConfig) {
        outcome = UiOutcome.Open(screenId, DialogBinds().also(binds::configure), values)
    }

    fun page(id: String) {
        outcome = UiOutcome.Page(id)
    }
}
