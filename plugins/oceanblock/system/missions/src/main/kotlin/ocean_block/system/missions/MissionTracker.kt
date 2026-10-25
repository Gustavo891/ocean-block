package ocean_block.system.missions

import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.level.PlayerLevels
import ocean_block.system.missions.data.MissionManager
import ocean_block.system.missions.hud.MissionHud
import ocean_block.system.missions.model.MissionSignal
import ocean_block.system.missions.model.PlayerMissions
import ocean_block.system.missions.notify.MissionNotifier
import ocean_block.system.missions.rules.MissionRules
import ocean_block.system.missions.text.MissionText
import org.bukkit.GameMode
import org.bukkit.entity.Player
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

object MissionTracker {

    val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun today(): LocalDate = LocalDate.now(zone)

    fun signal(player: Player, signal: MissionSignal) {
        if (!counts(player)) return
        val uuid = player.uniqueId
        val before = refreshed(player, MissionManager.get(uuid))
        val (applied, done) = MissionRules.apply(before, signal)
        if (applied == MissionManager.get(uuid) && done.isEmpty()) return
        var state = applied
        val completed = done.toMutableList()
        if (done.isNotEmpty()) {
            val (chained, more) = MissionRules.apply(state, stateOf(player))
            state = refreshed(player, chained)
            completed += more
        }
        MissionManager.set(uuid, state)
        if (completed.isNotEmpty()) MissionNotifier.completed(player)
        MissionHud.completed(player, completed.map { MissionText.completed(it.mission, it.xp) })
        completed.forEach { PlayerLevels.award(player, it.xp) }
        MissionHud.show(player, MissionManager.get(uuid))
    }

    fun join(player: Player) {
        refresh(player)
        signal(player, stateOf(player))
        MissionHud.show(player, MissionManager.get(player.uniqueId))
    }

    fun refresh(player: Player): Boolean {
        val current = MissionManager.get(player.uniqueId)
        val next = refreshed(player, current)
        if (next == current) return false
        MissionManager.set(player.uniqueId, next)
        MissionHud.show(player, next)
        return true
    }

    fun skip(player: Player) {
        MissionManager.set(player.uniqueId, MissionRules.skip(MissionManager.get(player.uniqueId)))
        join(player)
    }

    fun reset(player: Player) {
        MissionManager.set(player.uniqueId, MissionRules.fresh(player.uniqueId.toString()))
        join(player)
    }

    fun stateOf(player: Player): MissionSignal.State {
        val island = IslandManager.getIslandByPlayer(player.uniqueId)
        val upgrades = island?.let { found -> IslandUpgrade.entries.associate { it.name to found.getUpgradeLevel(it) } }
        return MissionSignal.State(PlayerLevels.levelOf(player), upgrades, hasGenerator = false)
    }

    private fun counts(player: Player): Boolean =
        player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR

    private fun refreshed(player: Player, state: PlayerMissions): PlayerMissions {
        val today = today()
        if (!MissionRules.needsReset(state, today)) return state
        return state.copy(
            dailyDate = today.toString(),
            dailies = MissionRules.rollDailies(today, PlayerLevels.levelOf(player), Random.Default),
        )
    }
}
