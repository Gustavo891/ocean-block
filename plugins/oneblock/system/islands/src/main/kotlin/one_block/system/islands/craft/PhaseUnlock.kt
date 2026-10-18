package one_block.system.islands.craft

import one_block.system.craft.forge.RecipeUnlock
import one_block.system.islands.island.IslandManager
import one_block.system.islands.phase.PhaseManager
import org.bukkit.entity.Player

data class PhaseUnlock(
    val phaseId: String,
) : RecipeUnlock {

    private val requiredPriority: Int? get() = PhaseManager.get(phaseId)?.priority

    override fun isUnlocked(player: Player): Boolean {
        val required = requiredPriority ?: return false
        val current = currentPriority(player) ?: return false

        return current >= required
    }

    override fun requirementLines(player: Player): List<String> {
        val phaseName = PhaseManager.get(phaseId)?.prettyName ?: phaseId

        return if (isUnlocked(player)) {
            listOf("<dark_gray>▪ <green>✔ Fase $phaseName")
        } else {
            listOf("<dark_gray>▪ <red>✖ Fase $phaseName")
        }
    }

    private fun currentPriority(player: Player): Int? =
        IslandManager.getIslandByPlayer(player.uniqueId)?.getCurrentPhase()?.priority
}
