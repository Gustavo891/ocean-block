package one_block.textura.hud

import net.kyori.adventure.bossbar.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HudManager : Listener {

    /** Bossbar de um slot mais o conteúdo que a desenha; a secundária é opcional. */
    private class Entry(val bar: BossBar, var content: HudContent, var secondary: HudSecondary?)

    private val bars = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Entry>>()

    fun show(player: Player, slot: String, content: HudContent) {
        val playerBars = bars.getOrPut(player.uniqueId) { ConcurrentHashMap() }

        val existing = playerBars[slot]
        if (existing == null) {
            val bar = BossBar.bossBar(
                HudRenderer.render(content, null),
                content.progress.coerceIn(0f, 1f),
                content.color,
                content.overlay,
            )
            playerBars[slot] = Entry(bar, content, null)
            player.showBossBar(bar)
        } else {
            if (existing.content != content) {
                existing.content = content
                apply(existing)
            }
            existing.bar.addViewer(player)
        }
    }

    fun update(player: Player, slot: String, content: HudContent) {
        val entry = bars[player.uniqueId]?.get(slot) ?: return
        if (entry.content == content) return
        entry.content = content
        apply(entry)
    }

    /** Define a HUD secundária do [slot]; no-op se o slot não tiver HUD principal. */
    fun showSecondary(player: Player, slot: String, secondary: HudSecondary) {
        val entry = bars[player.uniqueId]?.get(slot) ?: return
        if (entry.secondary == secondary) return
        entry.secondary = secondary
        apply(entry)
    }

    fun hideSecondary(player: Player, slot: String) {
        val entry = bars[player.uniqueId]?.get(slot) ?: return
        if (entry.secondary == null) return
        entry.secondary = null
        apply(entry)
    }

    fun hide(player: Player, slot: String) {
        val entry = bars[player.uniqueId]?.remove(slot) ?: return
        player.hideBossBar(entry.bar)
    }

    fun hideAll(player: Player) {
        val map = bars.remove(player.uniqueId) ?: return
        map.values.forEach { player.hideBossBar(it.bar) }
    }

    fun has(player: Player, slot: String): Boolean =
        bars[player.uniqueId]?.containsKey(slot) == true

    fun hasSecondary(player: Player, slot: String): Boolean =
        bars[player.uniqueId]?.get(slot)?.secondary != null

    private fun apply(entry: Entry) {
        entry.bar.name(HudRenderer.render(entry.content, entry.secondary))
        entry.bar.progress(entry.content.progress.coerceIn(0f, 1f))
        entry.bar.color(entry.content.color)
        entry.bar.overlay(entry.content.overlay)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        bars.remove(event.player.uniqueId)
    }
}
