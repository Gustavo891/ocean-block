package one_block.textura.notify.service

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import one_block.textura.api.TexturaTags
import one_block.textura.notify.NotificationLine
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.notify.region.RegionLines
import one_block.textura.notify.render.NotificationKit
import one_block.textura.notify.render.NotificationRenderer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NotificationService(private val plugin: Plugin, initialKit: NotificationKit) : Listener {

    private class Viewer(val bar: BossBar) {
        val regions = LinkedHashMap<String, RegionLines>()
        var shown = false
        var lastName: Component? = null
    }

    private val renderer = NotificationRenderer(TexturaTags.miniMessage)
    private val viewers = ConcurrentHashMap<UUID, Viewer>()
    private val warned = ConcurrentHashMap.newKeySet<String>()
    private var task: BukkitTask? = null

    @Volatile
    private var regions: Map<String, NotificationRegion> = emptyMap()

    @Volatile
    private var kit: NotificationKit = initialKit

    @Volatile
    private var translations: Map<String, String> = emptyMap()

    fun start() {
        task?.cancel()
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tick() }, EXPIRY_PERIOD, EXPIRY_PERIOD)
    }

    fun stop() {
        task?.cancel()
        task = null
        viewers.forEach { (uuid, viewer) -> Bukkit.getPlayer(uuid)?.hideBossBar(viewer.bar) }
        viewers.clear()
    }

    fun regions(regions: List<NotificationRegion>) {
        this.regions = regions.associateBy { it.id }
        onMain {
            viewers.values.forEach { viewer -> viewer.regions.clear() }
            refreshAll()
        }
    }

    fun regionList(): List<NotificationRegion> = regions.values.toList()

    fun kit(kit: NotificationKit) {
        this.kit = kit
        onMain { refreshAll() }
    }

    fun translations(translations: Map<String, String>) {
        this.translations = translations
        onMain { refreshAll() }
    }

    fun push(player: Player, regionId: String, line: NotificationLine) {
        onMain {
            val region = regions[regionId]
            if (region == null) {
                if (warned.add("regiao:$regionId")) plugin.logger.warning("[notificacoes] Região '$regionId' não existe em configs/hud/notificacoes.yml")
                return@onMain
            }
            if (!player.isOnline) return@onMain
            if (line.icon != null && kit.icon(line.icon) == null && kit.textTops.isNotEmpty() && warned.add("icone:${line.icon}")) {
                plugin.logger.warning("[notificacoes] Ícone '${line.icon}' não está no pack; registre com NotificationApi.registerItemIcons antes do build")
            }
            val viewer = viewers.getOrPut(player.uniqueId) { Viewer(BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS)) }
            viewer.regions.getOrPut(region.id) { RegionLines(region.maxLines) }.push(line, now())
            refresh(player, viewer)
        }
    }

    fun clear(player: Player, regionId: String) {
        onMain {
            val viewer = viewers[player.uniqueId] ?: return@onMain
            if (viewer.regions[regionId]?.clear() == true) refresh(player, viewer)
        }
    }

    private fun tick() {
        val now = now()
        viewers.forEach { (uuid, viewer) ->
            val player = Bukkit.getPlayer(uuid)
            if (player == null) {
                viewers.remove(uuid)
                return@forEach
            }
            val expired = viewer.regions.values.map { it.expire(now) }.any { it }
            if (expired) refresh(player, viewer)
        }
    }

    private fun refreshAll() {
        viewers.forEach { (uuid, viewer) -> Bukkit.getPlayer(uuid)?.let { refresh(it, viewer) } }
    }

    private fun refresh(player: Player, viewer: Viewer) {
        val ordered = regions.values.toList()
        val lines = viewer.regions.mapValues { (_, state) -> state.lines().map { it.line } }
        if (lines.values.all { it.isEmpty() }) {
            if (viewer.shown) player.hideBossBar(viewer.bar)
            viewer.shown = false
            viewer.lastName = null
            return
        }

        val name = renderer.render(ordered, lines, kit) { key -> translations[key] }
        if (name != viewer.lastName) {
            viewer.bar.name(name)
            viewer.lastName = name
        }
        if (!viewer.shown) {
            BossBarOrder.showFirst(player, viewer.bar)
            viewer.shown = true
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        viewers.remove(event.player.uniqueId)
    }

    private fun now(): Long = Bukkit.getCurrentTick().toLong()

    private fun onMain(action: () -> Unit) {
        if (Bukkit.isPrimaryThread()) action() else Bukkit.getScheduler().runTask(plugin, Runnable(action))
    }

    private companion object {
        const val EXPIRY_PERIOD = 5L
    }
}

object BossBarOrder {

    fun showFirst(player: Player, bar: BossBar) {
        val others = player.activeBossBars().filter { it != bar }
        others.forEach(player::hideBossBar)
        player.showBossBar(bar)
        others.forEach(player::showBossBar)
    }
}
