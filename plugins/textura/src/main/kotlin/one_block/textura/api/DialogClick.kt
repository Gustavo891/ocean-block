package one_block.textura.api

import one_block.textura.dialog.node.UiOutcome
import org.bukkit.entity.Player

class DialogClick internal constructor(val player: Player) {

    internal var outcome: UiOutcome = UiOutcome.Refresh
        private set

    fun close() {
        outcome = UiOutcome.Close
    }

    fun open(screenId: String, values: Map<String, String> = emptyMap(), binds: DialogBinds.() -> Unit = {}) {
        outcome = UiOutcome.Open(screenId, DialogBinds().apply(binds), values)
    }

    fun page(id: String) {
        outcome = UiOutcome.Page(id)
    }
}
