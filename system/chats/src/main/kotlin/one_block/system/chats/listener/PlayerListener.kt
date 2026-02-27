package one_block.system.chats.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerListener : Listener {

    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {
        event.isCancelled = true

        val formattedMessage = Component.empty()
            .append(
                MiniMessage.miniMessage().deserialize("<white>${event.player.name}: ")
                    .decoration(TextDecoration.ITALIC, false)
            )
            .append(event.originalMessage().color(NamedTextColor.GRAY))

        Bukkit.getOnlinePlayers().forEach { it.sendMessage(formattedMessage) }
    }
}