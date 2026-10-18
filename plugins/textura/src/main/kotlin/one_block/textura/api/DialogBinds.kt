package one_block.textura.api

class DialogBinds {

    internal class ValueBind(val get: () -> String, val set: (String) -> Unit)

    internal class IndexBind(val get: () -> Int, val set: (Int) -> Unit)

    internal val values = mutableMapOf<String, ValueBind>()
    internal val indexes = mutableMapOf<String, IndexBind>()
    internal val buttons = mutableMapOf<String, (DialogClick) -> Unit>()

    internal val keys: Set<String> get() = values.keys + indexes.keys + buttons.keys

    fun switch(key: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        values[key] = ValueBind({ get().toString() }, { set(it == "true") })
    }

    fun segmented(key: String, get: () -> Int, set: (Int) -> Unit) {
        indexes[key] = IndexBind(get, set)
    }

    fun dropdown(key: String, get: () -> String, set: (String) -> Unit) {
        values[key] = ValueBind(get, set)
    }

    fun slider(key: String, get: () -> Int, set: (Int) -> Unit) {
        values[key] = ValueBind({ get().toString() }, { raw -> raw.toIntOrNull()?.let(set) })
    }

    fun value(key: String, get: () -> String, set: (String) -> Unit = {}) {
        values[key] = ValueBind(get, set)
    }

    fun text(key: String, value: () -> String) {
        values[key] = ValueBind(value) {}
    }

    fun button(key: String, onClick: (DialogClick) -> Unit) {
        buttons[key] = onClick
    }
}
