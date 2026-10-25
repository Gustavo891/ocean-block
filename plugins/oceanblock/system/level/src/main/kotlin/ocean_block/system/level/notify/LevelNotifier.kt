package ocean_block.system.level.notify

import net.kyori.adventure.title.Title
import ocean_block.libs.formatter.Mini
import ocean_block.system.craft.recipe.OceanRecipe
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration
import java.util.UUID

object LevelNotifier : Listener {

    const val ICON_MATERIAL = "experience_bottle"

    private const val ICON = "item:$ICON_MATERIAL"
    private const val XP_DURATION_TICKS = 60
    private const val LEVEL_DURATION_TICKS = 100

    private val tallies = mutableMapOf<UUID, XpTally>()

    fun xp(player: Player, amount: Int) {
        if (amount <= 0) return
        val tally = tallies.getOrPut(player.uniqueId) { XpTally(XP_DURATION_TICKS.toLong()) }
        val total = tally.add(amount, Bukkit.getCurrentTick().toLong())
        NotificationApi.push(player, "coleta", NotificationLine("<green>+$total</green> <white>XP</white>", ICON, XP_DURATION_TICKS, "coleta:xp"))
    }

    fun levelUp(player: Player, level: Int, unlocked: List<OceanRecipe>) {
        player.showTitle(
            Title.title(
                Mini.mm.deserialize("<aqua>NÍVEL $level"),
                Mini.mm.deserialize("<gray>Você subiu de nível!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(750)),
            ),
        )
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        val text = "<green>Nível $level!</green>" +
            if (unlocked.isEmpty()) "" else " <white>Liberado: ${unlocked.joinToString(", ") { it.name }}</white>"
        NotificationApi.push(player, "avisos", NotificationLine(text, ICON, LEVEL_DURATION_TICKS, "nivel"))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        tallies.remove(event.player.uniqueId)
    }
}
