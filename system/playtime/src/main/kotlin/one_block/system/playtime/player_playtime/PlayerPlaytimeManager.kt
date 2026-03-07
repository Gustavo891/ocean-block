package one_block.system.playtime.player_playtime

import one_block.libs.instance.ScheduledExecutor
import java.util.*
import java.util.concurrent.TimeUnit

object PlayerPlaytimeManager {

    private val players = mutableMapOf<UUID, PlayerPlaytime>()
    private val repository = PlayerPlaytimeRepository()

    fun onInit() {
        ScheduledExecutor.scheduleAtFixedRate({
            players.values.forEach {
                it.updateElapsedSeconds()
                repository.update(it)
            }
        }, 0, 20, TimeUnit.SECONDS)
    }

    fun load(uuid: UUID, name: String) {
        val playerPlaytime = repository.find(uuid) ?: PlayerPlaytime(uuid, name, 0L)
        players[uuid] = playerPlaytime
    }

    fun get(uuid: UUID) = players[uuid]

    fun save(uuid: UUID) {
        val playerPlaytime = players.remove(uuid) ?: return
        playerPlaytime.updateElapsedSeconds()

        repository.update(playerPlaytime)
    }

    fun topPlaytimeList() = repository.topPlaytimeList()
}