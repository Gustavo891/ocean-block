package one_block.textura.hud.anchor

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import one_block.textura.api.TitleApi
import one_block.textura.notify.service.BossBarOrder
import one_block.textura.hud.anchor.AnchorPalette.AQUA
import one_block.textura.hud.anchor.AnchorPalette.GOLD
import one_block.textura.hud.anchor.AnchorPalette.GRAY
import one_block.textura.hud.anchor.AnchorPalette.GREEN
import one_block.textura.hud.anchor.AnchorPalette.RED
import one_block.textura.hud.anchor.AnchorPalette.WHITE
import one_block.textura.hud.anchor.AnchorPalette.YELLOW
import one_block.textura.hud.anchor.ScreenAnchor.BOTTOM_LEFT
import one_block.textura.hud.anchor.ScreenAnchor.BOTTOM_RIGHT
import one_block.textura.hud.anchor.ScreenAnchor.MIDDLE_LEFT
import one_block.textura.hud.anchor.ScreenAnchor.MIDDLE_RIGHT
import one_block.textura.hud.anchor.ScreenAnchor.TOP_LEFT
import one_block.textura.hud.anchor.ScreenAnchor.TOP_RIGHT
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object HudAnchorProbe : Listener {

    private val NEAR_MARKER: TextColor = TextColor.color(AnchorMarker.GLYPH_RED, 0x11, 0x11)

    private val LINES = listOf(
        line(TOP_LEFT, 0, "TOPO ESQ" to WHITE),
        line(TOP_RIGHT, 0, "TOPO DIR" to WHITE),
        line(MIDDLE_LEFT, 0, "MEIO ESQ" to YELLOW),
        line(MIDDLE_RIGHT, 0, "MEIO DIR " to GOLD, "+1" to GREEN, " Tábua" to WHITE),
        line(MIDDLE_RIGHT, 1, "+3" to GREEN, " Corda" to WHITE),
        line(MIDDLE_RIGHT, 2, "-2" to RED, " Sucata" to GRAY),
        line(MIDDLE_RIGHT, 3, "+5" to AQUA, " Peixe" to WHITE),
        line(BOTTOM_LEFT, 0, "BASE ESQ" to GOLD),
        line(BOTTOM_RIGHT, 0, "BASE DIR" to AQUA),
    )

    private val CONTROLS = listOf(
        CenteredLine(0, "CONTROLE vanilla", NamedTextColor.WHITE),
        CenteredLine(1, "CONTROLE #FE1111", NEAR_MARKER),
    )

    private val bars = ConcurrentHashMap<UUID, BossBar>()

    fun registerFonts() {
        AnchoredLayout.ascents().forEach { TitleApi.registerAscent(it) }
    }

    fun render(): Component = AnchoredLayout.compose(LINES, CONTROLS)

    fun show(player: Player) {
        hide(player)
        val bar = BossBar.bossBar(render(), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS)
        bars[player.uniqueId] = bar
        BossBarOrder.showFirst(player, bar)
    }

    fun hide(player: Player) {
        bars.remove(player.uniqueId)?.let(player::hideBossBar)
    }

    fun isShown(player: Player): Boolean = bars.containsKey(player.uniqueId)

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        bars.remove(event.player.uniqueId)
    }

    private fun line(anchor: ScreenAnchor, row: Int, vararg spans: Pair<String, AnchorPalette>): AnchorLine =
        AnchorLine(anchor, row, spans.map { (text, palette) -> AnchorSpan(text, palette) })
}
