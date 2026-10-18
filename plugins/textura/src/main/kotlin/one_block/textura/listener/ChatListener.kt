package one_block.textura.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import one_block.textura.TexturaPlugin
import java.util.regex.Pattern

class ChatListener(private val plugin: TexturaPlugin) : Listener {

    private val replacement = TextReplacementConfig.builder()
        .match(Pattern.compile(":([a-zA-Z0-9_\\-]+):"))
        .replacement { result, _ ->
            val id = result.group(1)
            val glyph = plugin.glyphRegistry.glyphs[id]?.takeIf { it.default && it.height <= MAX_CHAT_HEIGHT }

            if (glyph != null) {
                Component.text(glyph.char)
                    .color(NamedTextColor.WHITE)
                    .hoverEvent(Component.text(":$id:"))
            } else {
                Component.text(":$id:")
            }
        }
        .build()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        if (!event.player.hasPermission(PERMISSION)) return
        event.message(event.message().replaceText(replacement))
    }

    private companion object {
        const val PERMISSION = "resourcepack.emojis"
        const val MAX_CHAT_HEIGHT = 16
    }
}
