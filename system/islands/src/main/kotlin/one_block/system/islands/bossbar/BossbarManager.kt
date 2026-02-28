package one_block.system.islands.bossbar

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.system.islands.island.Island
import org.bukkit.Bukkit
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

        val textPlain = "<white>Fase: <gray>${phase.prettyName} <yellow>(${island.grid}/${phase.blocksToNextPhase})"

        return MiniMessage.miniMessage().deserialize("<glyph:floating_islands> <font:minecraft:test>$textPlain")
    }

    fun getBossBar(island: Island): BossBar? {
        return bossbars[island.uuid]
    }

}