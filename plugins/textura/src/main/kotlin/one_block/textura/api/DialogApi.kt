package one_block.textura.api

import one_block.textura.dialog.DialogOpener
import org.bukkit.entity.Player

object DialogApi {

    @Volatile
    private var opener: DialogOpener? = null

    fun init(opener: DialogOpener) {
        this.opener = opener
    }

    fun open(
        player: Player,
        screenId: String,
        values: Map<String, String> = emptyMap(),
        binds: DialogBinds.() -> Unit = {},
    ): Boolean = opener?.open(player, screenId, values, DialogBinds().apply(binds)) ?: false

    fun setting(player: Player, key: String): String? = opener?.setting(player, key)

    fun setSetting(player: Player, key: String, value: String) {
        opener?.setSetting(player, key, value)
    }

    fun close(player: Player) {
        opener?.close(player)
    }
}
