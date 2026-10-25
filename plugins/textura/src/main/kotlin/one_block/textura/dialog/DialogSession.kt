package one_block.textura.dialog

import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogClick
import one_block.textura.dialog.node.UiOutcome
import org.bukkit.entity.Player
import java.awt.image.BufferedImage

class DialogSession(
    val screenId: String,
    internal val binds: DialogBinds,
    initial: Map<String, String> = emptyMap(),
    val player: Player? = null,
    private val settings: PlayerSettings? = null,
) {

    var page: String? = null
    var persistent: Set<String> = emptySet()
    var defaults: Map<String, String> = emptyMap()
    var choices: Map<String, List<String>> = emptyMap()

    private val values = initial.toMutableMap()

    fun value(key: String): String? {
        binds.values[key]?.let { return it.get() }
        binds.indexes[key]?.let { bind ->
            choices[key]?.getOrNull(bind.get())?.let { return DialogValues.optionId(it) }
        }
        if (key in persistent && player != null) settings?.get(player, key)?.let { return it }
        return values[key] ?: defaults[key]
    }

    fun set(key: String, value: String) {
        val bind = binds.values[key]
        val indexBind = binds.indexes[key]
        val index = choices[key]?.indexOfFirst { DialogValues.optionId(it) == value } ?: -1
        when {
            bind != null -> bind.set(value)
            indexBind != null && index >= 0 -> indexBind.set(index)
            key in persistent && player != null && settings != null -> settings.set(player, key, value)
            else -> values[key] = value
        }
    }

    fun choice(key: String, options: List<String>, default: Int): Int {
        binds.indexes[key]?.let { return it.get().coerceIn(0, options.lastIndex) }
        val current = value(key) ?: return default
        return options.indexOfFirst { DialogValues.optionId(it) == current }.takeIf { it >= 0 } ?: default
    }

    fun choose(key: String, options: List<String>, index: Int) {
        val bind = binds.indexes[key]
        if (bind != null) bind.set(index) else set(key, DialogValues.optionId(options[index]))
    }

    fun carried(): Map<String, String> = values.toMap()

    fun press(key: String, player: Player): UiOutcome {
        val handler = binds.buttons[key] ?: return UiOutcome.Refresh
        val click = DialogClick(player)
        handler.run(click)
        return click.outcome
    }

    fun press(key: String, index: Int, player: Player): UiOutcome {
        val handler = binds.listButtons[key] ?: return press(key, player)
        val click = DialogClick(player)
        handler.run(click, index)
        return click.outcome
    }

    fun entries(list: String): List<Map<String, String>> = binds.lists[list]?.get().orEmpty()

    fun listPage(list: String): Int = binds.listPages[list]?.asInt ?: 0

    fun pixels(key: String): BufferedImage? = binds.pixels[key]?.get()
}
