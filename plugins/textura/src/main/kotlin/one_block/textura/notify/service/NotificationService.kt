package one_block.textura.notify.service

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import one_block.textura.api.TexturaTags
import one_block.textura.notify.NotificationLine
import one_block.textura.notify.region.NotificationAnimation
import one_block.textura.notify.region.NotificationRegion
import one_block.textura.notify.region.RegionLines
import one_block.textura.notify.render.NotificationKit
import one_block.textura.notify.render.NotificationRenderer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class NotificationService(private val plugin: Plugin, initialKit: NotificationKit) : Listener {

    private class Viewer(val bar: BossBar) {
        val regions = LinkedHashMap<String, RegionLines>()
        var lastName: Component = Component.empty()
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
        Bukkit.getOnlinePlayers().forEach(::viewer)
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

    fun translation(key: String): String? = translations[key]

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
            val viewer = viewer(player)
            viewer.regions.getOrPut(region.id) { RegionLines(region.maxLines) }.push(line, now())
            refresh(viewer)
        }
    }

    fun clear(player: Player, regionId: String) {
        onMain {
            val viewer = viewers[player.uniqueId] ?: return@onMain
            if (viewer.regions[regionId]?.clear() == true) refresh(viewer)
        }
    }

    private fun viewer(player: Player): Viewer = viewers.getOrPut(player.uniqueId) {
        val viewer = Viewer(BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS))
        BossBarOrder.showFirst(player, viewer.bar)
        viewer
    }

    private fun tick() {
        val now = now()
        viewers.forEach { (uuid, viewer) ->
            if (Bukkit.getPlayer(uuid) == null) {
                viewers.remove(uuid)
                return@forEach
            }
            val expired = viewer.regions.values.map { it.expire(now) }.any { it }
            val animating = viewer.regions.any { (id, state) -> regions[id]?.animated == true && !state.isEmpty() }
            if (expired || animating) refresh(viewer)
        }
    }

    private fun refreshAll() {
        viewers.values.forEach(::refresh)
    }

    private fun refresh(viewer: Viewer) {
        val now = now()
        val lines = viewer.regions.mapValues { (_, state) -> state.lines().map { it.line } }
        val frames = viewer.regions.mapValues { (id, state) ->
            if (regions[id]?.animated == true) state.lines().map { NotificationAnimation.frame(now - it.startedAt, it.line.durationTicks) } else emptyList()
        }
        val name = if (lines.values.all { it.isEmpty() }) {
            Component.empty()
        } else {
            renderer.render(regions.values.toList(), lines, kit, frames) { key -> translations[key] }
        }
        if (name != viewer.lastName) {
            viewer.bar.name(name)
            viewer.lastName = name
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        viewer(event.player)
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
        const val EXPIRY_PERIOD = 2L
    }
}

internal object BossBarOrder {

    fun showFirst(player: Player, bar: BossBar) {
        val others = player.activeBossBars().filter { it != bar }
        others.forEach(player::hideBossBar)
        player.showBossBar(bar)
        others.forEach(player::showBossBar)
    }
}
