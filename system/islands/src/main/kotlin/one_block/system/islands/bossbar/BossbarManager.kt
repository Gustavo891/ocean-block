package one_block.system.islands.bossbar

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.system.islands.island.Island
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object BossbarManager {

    private val bossbars = mutableMapOf<UUID, BossBar>()

    fun initBossBar(island: Island) {
        val phase = island.getCurrentPhase() ?: return

        val progression = island.grid.toFloat() / phase.blocksToNextPhase.toFloat()

        val bossbar = BossBar.bossBar(
            updateProgression(island),
            progression,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        )

        bossbars[island.uuid] = bossbar
    }

    fun upgradeBossBar(island: Island) {
        val bossbar = bossbars[island.uuid] ?: return
        val phase = island.getCurrentPhase() ?: return
        val progression = island.grid.toFloat() / phase.blocksToNextPhase.toFloat()

        bossbar.progress(progression)
        bossbar.name(updateProgression(island))
    }

    fun updateProgression(island: Island): Component {
        val phase = island.getCurrentPhase() ?: return Component.empty()

        val textPlain = "<white>Fase: <gray>${phase.prettyName} <dark_gray>(${island.grid}/${phase.blocksToNextPhase})"

        return MiniMessage.miniMessage().deserialize("<glyph:floating_islands> <font:oneblock:test>$textPlain")
    }

    fun updatePlayerBar(player: Player, island: Island) {
        player.activeBossBars().forEach { it.removeViewer(player) }
        if(getBossBar(island) == null) {
            initBossBar(island)
        }
        getBossBar(island)?.addViewer(player)
    }

    fun getBossBar(island: Island): BossBar? {
        return bossbars[island.uuid]
    }

    fun clearBossbar(island: Island) {
        val bossbar = bossbars[island.uuid] ?: return
        bossbar.viewers()
            .filterIsInstance<Player>()
            .forEach { bossbar.removeViewer(it) }
        bossbars.remove(island.uuid)
    }

}