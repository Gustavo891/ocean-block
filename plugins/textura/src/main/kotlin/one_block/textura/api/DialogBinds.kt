package one_block.textura.api

import java.awt.image.BufferedImage
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.IntConsumer
import java.util.function.IntSupplier
import java.util.function.Supplier

class DialogBinds {

    internal class ValueBind(private val reader: Supplier<String>, private val writer: Consumer<String>) {
        fun get(): String = reader.get()
        fun set(value: String) = writer.accept(value)
    }

    internal class IndexBind(private val reader: IntSupplier, private val writer: IntConsumer) {
        fun get(): Int = reader.asInt
        fun set(value: Int) = writer.accept(value)
    }

    internal val values = mutableMapOf<String, ValueBind>()
    internal val indexes = mutableMapOf<String, IndexBind>()
    internal val buttons = mutableMapOf<String, DialogButtonAction>()
    internal val lists = mutableMapOf<String, Supplier<List<Map<String, String>>>>()
    internal val listPages = mutableMapOf<String, IntSupplier>()
    internal val listButtons = mutableMapOf<String, DialogListAction>()
    internal val pixels = mutableMapOf<String, Supplier<BufferedImage>>()
    internal var closed: Runnable? = null

    internal val keys: Set<String> get() = values.keys + indexes.keys + buttons.keys + lists.keys + listPages.keys + listButtons.keys + pixels.keys

    fun switch(key: String, get: BooleanSupplier, set: Consumer<Boolean>) {
        values[key] = ValueBind({ get.asBoolean.toString() }, { set.accept(it == "true") })
    }

    fun segmented(key: String, get: IntSupplier, set: IntConsumer) {
        indexes[key] = IndexBind(get, set)
    }

    fun dropdown(key: String, get: Supplier<String>, set: Consumer<String>) {
        values[key] = ValueBind(get, set)
    }

    fun slider(key: String, get: IntSupplier, set: IntConsumer) {
        values[key] = ValueBind({ get.asInt.toString() }, { raw -> raw.toIntOrNull()?.let(set::accept) })
    }

    fun value(key: String, get: Supplier<String>) {
        values[key] = ValueBind(get) {}
    }

    fun value(key: String, get: Supplier<String>, set: Consumer<String>) {
        values[key] = ValueBind(get, set)
    }

    fun text(key: String, value: Supplier<String>) {
        values[key] = ValueBind(value) {}
    }

    fun button(key: String, onClick: DialogButtonAction) {
        buttons[key] = onClick
    }

    fun list(key: String, entries: Supplier<List<Map<String, String>>>) {
        lists[key] = entries
    }

    fun listPage(key: String, page: IntSupplier) {
        listPages[key] = page
    }

    fun listButton(key: String, action: DialogListAction) {
        listButtons[key] = action
    }

    fun pixels(key: String, image: Supplier<BufferedImage>) {
        pixels[key] = image
    }

    fun closed(action: Runnable) {
        closed = action
    }
}
