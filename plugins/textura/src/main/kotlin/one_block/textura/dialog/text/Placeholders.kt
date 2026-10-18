package one_block.textura.dialog.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.DialogSession
import org.bukkit.Bukkit

class Placeholders(private val session: DialogSession, private val extra: Map<String, String> = emptyMap()) {

    private class Piece(val key: String, val rendered: String, val untrusted: String?, val branch: String? = null, val value: String? = null)

    fun plain(text: String): String =
        expand(text) { it.rendered.replace('\n', ' ').replace('\r', ' ') }

    fun rich(text: String): Component {
        val resolvers = mutableListOf<TagResolver>()
        val converted = expand(text) { piece ->
            val tag = "bind_${resolvers.size}"
            if (piece.branch != null) {
                resolvers += Placeholder.unparsed(tag, piece.value.orEmpty())
                piece.branch.replace(VALUE_MARK, "<$tag>")
            } else {
                resolvers += Placeholder.unparsed(tag, piece.rendered)
                "<$tag>"
            }
        }
        return TexturaTags.miniMessage.deserialize(converted, TagResolver.resolver(resolvers))
    }

    fun command(text: String, onUnsafe: (String) -> Unit): String? {
        var refused: String? = null
        val result = expand(text) { piece ->
            if (refused == null && piece.untrusted != null && !SAFE.matches(piece.untrusted)) refused = piece.key
            piece.rendered
        }
        refused?.let {
            onUnsafe(it)
            return null
        }
        return result
    }

    private fun expand(text: String, render: (Piece) -> String): String =
        TOKEN.replace(text) { match ->
            val papi = match.groupValues[3]
            if (papi.isNotEmpty()) {
                val value = Papi.resolve(session.player, papi) ?: return@replace match.value
                return@replace render(Piece(papi, value, value))
            }
            val key = match.groupValues[1]
            val modifier = match.groupValues[2]
            val value = resolve(key)
            when {
                modifier.startsWith("?") -> {
                    val body = modifier.substring(1)
                    val branch = choose(body, value)
                    val chosen = branch.replace(VALUE_MARK, value.orEmpty())
                    render(Piece(key, chosen, if (body.contains(VALUE_MARK)) value else null, branch, value))
                }
                value == null -> match.value
                modifier == RAW -> render(Piece(key, value, null))
                else -> render(Piece(key, value, value))
            }
        }

    private fun choose(body: String, value: String?): String {
        val entries = body.split('|')
        entries.forEach { entry ->
            val colon = entry.indexOf(':')
            if (colon >= 0 && entry.substring(0, colon) == value) return entry.substring(colon + 1)
        }
        return entries.lastOrNull { !it.contains(':') }.orEmpty()
    }

    private fun resolve(key: String): String? = extra[key] ?: builtin(key) ?: session.value(key)

    private fun builtin(key: String): String? = when (key) {
        "player" -> session.player?.name.orEmpty()
        "player_uuid" -> session.player?.uniqueId?.toString().orEmpty()
        "online" -> Bukkit.getOnlinePlayers().size.toString()
        "page" -> session.page.orEmpty()
        else -> null
    }

    companion object {
        const val VALUE_KEY = "value"
        private const val RAW = "|raw"
        private const val VALUE_MARK = "\$value"
        private val SAFE = Regex("[A-Za-z0-9_.:-]*")
        val TOKEN = Regex("""\{([A-Za-z0-9_.-]+)(\?[^}]*|\|raw)?}|%([A-Za-z0-9_.:-]+)%""")
    }
}
