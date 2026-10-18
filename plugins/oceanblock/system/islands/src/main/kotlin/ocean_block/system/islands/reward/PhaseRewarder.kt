package ocean_block.system.islands.reward

import ocean_block.libs.formatter.Mini
import ocean_block.libs.instance.Instance
import ocean_block.libs.rewards.RewardGranter
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.phase.Phase
import ocean_block.system.islands.phase.PhaseManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object PhaseRewarder {

    private val mini = Mini.mm

    fun grantOrQueue(island: Island, phase: Phase) {
        val owner = Bukkit.getPlayer(island.ownerUuid)

        if (owner == null) {
            island.pendingRewardPhases.add(phase.id)
            return
        }

        grant(phase, listOf(owner))
    }

    fun drainPending(owner: Player, island: Island) {
        if (island.pendingRewardPhases.isEmpty()) return

        val pending = island.pendingRewardPhases.toList()
        island.pendingRewardPhases.clear()

        for (phaseId in pending) {
            val phase = PhaseManager.get(phaseId)

            if (phase == null) {
                Instance.logger.warning("[phase] Recompensa pendente de fase inexistente: $phaseId")
                continue
            }

            owner.sendMessage(
                mini.deserialize("<gray>Recompensa pendente da fase <yellow>${phase.prettyName}<gray>:")
            )
            grant(phase, listOf(owner))
        }
    }

    fun grant(phase: Phase, players: List<Player>) = RewardGranter.grant(phase.rewards, players)
}
