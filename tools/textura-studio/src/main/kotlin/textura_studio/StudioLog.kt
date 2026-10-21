package textura_studio

import java.util.Collections
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/** Logger que guarda os avisos do motor do Textura para mostrar no painel. */
class StudioLog {

    private val messages = Collections.synchronizedList(mutableListOf<String>())

    val logger: Logger = Logger.getAnonymousLogger().apply {
        useParentHandlers = false
        level = Level.ALL
        addHandler(object : Handler() {
            override fun publish(record: LogRecord) {
                if (record.level.intValue() >= Level.WARNING.intValue()) add(record.message)
            }

            override fun flush() = Unit

            override fun close() = Unit
        })
    }

    fun add(message: String) {
        if (message !in messages) messages += message
    }

    fun all(): List<String> = synchronized(messages) { messages.toList() }
}
