package one_block.system.combatlog.combat_player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object CombatPlayerManager {

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

        player.sendActionBar(Component.empty())
        player.sendRichMessage("<yellow>Você saiu de combate!")

        return combatPlayer
    }

    private fun processExpiredCombats() {
        for (entry in players.entries) {
            val player = Bukkit.getPlayer(entry.key) ?: continue

            val combatPlayer = entry.value
            if (!combatPlayer.isExpired) {
                player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Você está em combate por <b>${combatPlayer.secondsRemaining}</b>s").decoration(
                    TextDecoration.ITALIC, false))
                continue
            }

            players.remove(entry.key)

            player.sendActionBar(Component.empty())
            player.sendRichMessage("<yellow>Você saiu de combate!")
        }
    }
}