package one_block.system.scoreboard.board

import one_block.libs.formatter.Mini
import one_block.libs.instance.Instance
import one_block.libs.scoreboard.ScoreboardApi
import one_block.libs.scoreboard.SidebarLine
import one_block.system.scoreboard.config.BoardDefinition
import one_block.system.scoreboard.config.ScoreboardConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BoardManager {

    private val hidden = ConcurrentHashMap.newKeySet<UUID>()

    private var task: BukkitTask? = null

    private var ticks = 0L

    fun start() {
        stop()
        if (!ScoreboardConfig.enabled) return

        task = Bukkit.getScheduler().runTaskTimer(Instance, Runnable { tick() }, 1L, 1L)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun reload() {
        ScoreboardConfig.reload()

        if (!ScoreboardConfig.enabled) {
            stop()
            Bukkit.getOnlinePlayers().forEach { ScoreboardApi.hide(it) }
            return
        }

        start()
        Bukkit.getOnlinePlayers().forEach { render(it) }
    }

    fun toggle(player: Player): Boolean {
        val visible = hidden.remove(player.uniqueId)
        if (visible) {
            render(player)
            return true
        }

        hidden.add(player.uniqueId)
        ScoreboardApi.hide(player)
        return false
    }

    fun isHidden(player: Player): Boolean = hidden.contains(player.uniqueId)

    fun forget(player: Player) {
        hidden.remove(player.uniqueId)
    }

    fun render(player: Player) {
        val board = boardFor(player) ?: return
        draw(player, board)
    }

    private fun tick() {
        ticks++

        for (player in Bukkit.getOnlinePlayers()) {
            val board = boardFor(player) ?: continue
            if (ticks % board.renderEvery != 0L) continue

            draw(player, board)
        }
    }

    private fun boardFor(player: Player): BoardDefinition? {
        if (!ScoreboardConfig.enabled) return null
        if (isHidden(player)) return null

        return ScoreboardConfig.boardFor(player)
    }

    private fun draw(player: Player, board: BoardDefinition) {
        val frame = ((ticks / board.renderEvery) % board.titles.size).toInt()
        val title = Mini.deserialize(Placeholders.apply(player, board.titles[frame]))

        val lines = board.scores.map { line ->
            SidebarLine(line.score, Mini.deserialize(Placeholders.apply(player, line.text)))
        }

        ScoreboardApi.show(player, title, lines, board.hideNumber)
    }
}
