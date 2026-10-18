package one_block.system.coin_flip.coin_flip

import net.kyori.adventure.text.Component
import one_block.libs.instance.Instance
import one_block.system.currencies.Currencies
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import one_block.libs.formatter.Mini

object CoinFlipManager {

    val RESULT_DELAY: Duration = Duration.ofSeconds(5)

    private val openGames = ConcurrentHashMap<UUID, CoinFlip>()
    private val runningGames = ConcurrentHashMap<UUID, CoinFlip>()
    private val repository = CoinFlipRepository()

    fun onInit() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate({

            val now = Instant.now()

            forEachRunning { game ->
                if (game.shouldFinish(now, RESULT_DELAY)) {
                    Bukkit.getScheduler().runTask(Instance, Runnable { finishGame(game) })
                }
            }

        }, 1L, 1L, TimeUnit.SECONDS)

        repository.toList().forEach { openGames[it.uuid] = it }
    }

    fun onDisable() {
        repository.updateAll(toList())
    }

    fun toList() = openGames.values.toMutableList()

    fun createGame(player: Player, amount: Double): CoinFlip {
        val game = CoinFlip(UUID.randomUUID(), player.uniqueId, player.name, amount)

        openGames[game.uuid] = game
        return game
    }

    fun joinGame(gameId: UUID, opponent: Player): CoinFlip? {
        val game = openGames.remove(gameId) ?: return null
        game.opponent = opponent.uniqueId
        game.startInstant = Instant.now()

        runningGames[game.uuid] = game
        opponent.sendRichMessage("<green>Você aceitou o coinflip do jogador ${game.creatorName}, Boa sorte!")
        opponent.playSound(opponent, Sound.BLOCK_LEVER_CLICK, 1f, 1f)

        return game
    }

    fun finishGame(game: CoinFlip) {
        val creator = Bukkit.getPlayer(game.creator)
        val opponent = Bukkit.getPlayer(game.opponent ?: return)

        if (creator == null || opponent == null) {
            runningGames.remove(game.uuid)
            return
        }

        val winner = if (Random().nextBoolean()) creator else opponent
        val loser = if (winner.uniqueId == creator.uniqueId) opponent else creator

        val prize = game.amount * 2
        Currencies.deposit(winner.uniqueId, "coins", prize)

        val message = Component.text {
            it.appendNewline()
            it.append(Mini.mm.deserialize(" <color:#ffff2e>Coin Flip!</color>"))
            it.appendNewline()
            it.append(
                Mini.mm
                    .deserialize(" <green>${winner.name}</green> <white>levou tudo de <red>${loser.name}</red>! <green>\$<white>${prize}</white></green>")
            )
            it.appendNewline()
        }

        Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }

        runningGames.remove(game.uuid)
    }

    fun forEachRunning(action: (CoinFlip) -> Unit) {
        runningGames.values.forEach(action)
    }
}
