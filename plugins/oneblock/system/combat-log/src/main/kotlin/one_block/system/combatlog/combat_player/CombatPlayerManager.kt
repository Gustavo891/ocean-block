package one_block.system.combatlog.combat_player

import net.kyori.adventure.text.format.TextDecoration
import one_block.libs.actionbar.ActionBar
import one_block.libs.actionbar.Channel
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import one_block.libs.formatter.Mini

object CombatPlayerManager {

    private const val ACTION_BAR_TICKS = 25

    private val players = ConcurrentHashMap<UUID, CombatPlayer>()

    init {
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(::processExpiredCombats, 0L, 1L, TimeUnit.SECONDS)
    }

    fun insert(attacker: Player, receiver: Player) {
        players.computeIfAbsent(attacker.uniqueId) {
            CombatPlayer(it)
        }.apply {
            renewCombat(receiver.name)
        }

        players.computeIfAbsent(receiver.uniqueId) {
            CombatPlayer(it)
        }.apply {
            renewCombat(attacker.name)
        }
    }

    fun isPlayerInCombat(player: Player): Boolean {
        return players.containsKey(player.uniqueId)
    }

    fun removeCombat(player: Player): CombatPlayer? {
        val combatPlayer = players.remove(player.uniqueId) ?: return null

        ActionBar.clear(player, Channel.Combat)
        player.sendRichMessage("<yellow>Você saiu de combate!")

        return combatPlayer
    }

    private fun processExpiredCombats() {
        for (entry in players.entries) {
            val player = Bukkit.getPlayer(entry.key) ?: continue

            val combatPlayer = entry.value
            if (!combatPlayer.isExpired) {
                ActionBar.send(
                    player,
                    Channel.Combat,
                    Mini.mm
                        .deserialize("<red>Você está em combate por <b>${combatPlayer.secondsRemaining}</b>s")
                        .decoration(TextDecoration.ITALIC, false),
                    ACTION_BAR_TICKS,
                )
                continue
            }

            players.remove(entry.key)

            ActionBar.clear(player, Channel.Combat)
            player.sendRichMessage("<yellow>Você saiu de combate!")
        }
    }
}
