package one_block.textura.api

import one_block.textura.dialog.DialogOpener
import one_block.textura.dialog.kit.DynamicItemIcons
import org.bukkit.entity.Player

object DialogApi {

    @Volatile
    private var opener: DialogOpener? = null

    fun init(opener: DialogOpener) {
        this.opener = opener
    }

    fun open(player: Player, screenId: String): Boolean =
        openResult(player, screenId) == DialogOpenResult.Shown

    fun open(player: Player, screenId: String, binds: DialogBindsConfig): Boolean =
        openResult(player, screenId, binds) == DialogOpenResult.Shown

    fun open(player: Player, screenId: String, values: Map<String, String>, binds: DialogBindsConfig): Boolean =
        openResult(player, screenId, values, binds) == DialogOpenResult.Shown

    fun openResult(player: Player, screenId: String): DialogOpenResult =
        openResult(player, screenId, emptyMap()) {}

    fun openResult(player: Player, screenId: String, binds: DialogBindsConfig): DialogOpenResult =
        openResult(player, screenId, emptyMap(), binds)

    fun openResult(player: Player, screenId: String, values: Map<String, String>, binds: DialogBindsConfig): DialogOpenResult =
        opener?.open(player, screenId, values, DialogBinds().also(binds::configure)) ?: DialogOpenResult.Failed

    fun registerItemIcons(materials: Collection<String>) {
        DynamicItemIcons.register(materials)
    }

    fun setting(player: Player, key: String): String? = opener?.setting(player, key)

    fun setSetting(player: Player, key: String, value: String) {
        opener?.setSetting(player, key, value)
    }

    fun close(player: Player) {
        opener?.close(player)
    }
}
