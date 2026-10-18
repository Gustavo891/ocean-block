package one_block.libs.actionbar

import net.kyori.adventure.text.Component
import one_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ActionBar {

    private const val RENDER_INTERVAL_TICKS = 2L

    private const val REFRESH_TICKS = 20

    private data class Entry(val text: Component, val expiresAt: Int)

    private data class Showing(val text: Component, val sentAt: Int)

    private val entries = ConcurrentHashMap<UUID, MutableMap<Channel, Entry>>()
    private val showing = ConcurrentHashMap<UUID, Showing>()

    @Volatile
    private var started = false

    fun send(player: Player, channel: Channel, text: Component, durationTicks: Int) {
        start()

        entries.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }[channel] =
            Entry(text, Bukkit.getCurrentTick() + durationTicks)
    }

    fun clear(player: Player, channel: Channel) {
        entries[player.uniqueId]?.remove(channel)
    }

    private fun start() {
        if (started) return
        started = true

        Bukkit.getScheduler().runTaskTimer(
            Instance,
            Runnable { render() },
            RENDER_INTERVAL_TICKS,
            RENDER_INTERVAL_TICKS,
        )
    }

    private fun render() {
        if (entries.isEmpty()) return

        val now = Bukkit.getCurrentTick()
        val iterator = entries.entries.iterator()

        while (iterator.hasNext()) {
            val (uuid, channels) = iterator.next()

            channels.values.removeIf { it.expiresAt <= now }

            val player = Bukkit.getPlayer(uuid)
            if (player == null) {
                iterator.remove()
                showing.remove(uuid)
                continue
            }

            val top = channels.entries.maxByOrNull { it.key.priority }?.value

            if (top == null) {
                iterator.remove()
                if (showing.remove(uuid) != null) player.sendActionBar(Component.empty())
                continue
            }

            val current = showing[uuid]
            if (current != null && current.text == top.text && now - current.sentAt < REFRESH_TICKS) continue

            player.sendActionBar(top.text)
            showing[uuid] = Showing(top.text, now)
        }
    }
}
