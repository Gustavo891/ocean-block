package ocean_block.system.skills.feedback

import ocean_block.libs.actionbar.ActionBar
import ocean_block.libs.actionbar.Channel
import ocean_block.libs.formatter.Mini
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.instance.Instance
import ocean_block.system.skills.skill.Skill
import ocean_block.system.skills.skill.SkillManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

object XpFeedback {

    private const val FLUSH_INTERVAL_TICKS = 5L

    private const val ACTION_BAR_TICKS = 40

    private const val FALLBACK_FORWARD = 1.2

    private data class Pending(var skill: Skill, var amount: Long, var at: Location?)

    private val pending = mutableMapOf<UUID, Pending>()

    fun start() {
        Bukkit.getScheduler().runTaskTimer(
            Instance,
            Runnable { flushAll() },
            FLUSH_INTERVAL_TICKS,
            FLUSH_INTERVAL_TICKS,
        )
    }

    fun record(player: Player, skill: Skill, amount: Long, at: Location? = null) {
        if (amount <= 0) return

        val current = pending[player.uniqueId]

        if (current != null && current.skill != skill) {
            show(player, current)
            pending.remove(player.uniqueId)
        }

        val entry = pending.getOrPut(player.uniqueId) { Pending(skill, 0L, at) }
        entry.amount += amount
        if (at != null) entry.at = at
    }

    private fun flushAll() {
        if (pending.isEmpty()) return

        val snapshot = pending.toList()
        pending.clear()

        for ((uuid, entry) in snapshot) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            show(player, entry)
        }
    }

    private fun show(player: Player, entry: Pending) {
        if (entry.amount <= 0) return

        val amount = NumberFormatter.format(entry.amount.toDouble())

        ActionBar.send(player, Channel.SkillXp, Mini.deserialize(actionBarText(player, entry)), ACTION_BAR_TICKS)

        XpTextDisplay.send(
            player,
            Mini.deserialize("<green>+$amount XP"),
            entry.at ?: frontOf(player),
        )
    }

    private fun actionBarText(player: Player, entry: Pending): String {
        val amount = NumberFormatter.format(entry.amount.toDouble())
        val data = SkillManager.get(player.uniqueId)
        val level = data?.levelOf(entry.skill) ?: 0

        val progress = if (data == null || level >= Skill.MAX_LEVEL) {
            "<gold>MÁX"
        } else {
            val current = NumberFormatter.format(data.progressOf(entry.skill).toDouble())
            val needed = NumberFormatter.format(data.nextLevelCost(entry.skill).toDouble())
            "<gray>$current<dark_gray>/<gray>$needed"
        }

        return "<dark_gray>[<yellow>Lvl.$level<dark_gray>] <white>${entry.skill.prettyName} " +
            "<dark_gray>($progress<dark_gray>) <green>+$amount XP"
    }

    private fun frontOf(player: Player): Location =
        player.eyeLocation.clone().add(player.location.direction.multiply(FALLBACK_FORWARD))
}
