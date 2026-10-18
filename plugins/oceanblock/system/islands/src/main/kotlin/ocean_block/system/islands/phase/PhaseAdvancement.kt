package ocean_block.system.islands.phase

import ocean_block.libs.formatter.Mini
import ocean_block.system.islands.ext.getRandomByWeight
import ocean_block.system.islands.hud.IslandHud
import ocean_block.system.islands.ext.runNextTask
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.reward.PhaseRewarder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID

object PhaseAdvancement {

    private const val ISLAND_COMPLETE = "<gradient:#f7971e:#ffd200>Ilha completa!</gradient> <gray>Voce concluiu todas as fases."

    private val frozen = mutableSetOf<UUID>()

    fun isFrozen(islandId: UUID): Boolean = islandId in frozen

    fun clear(islandId: UUID) {
        frozen.remove(islandId)
        PhaseAnimation.cancel(islandId)
    }

    fun setPhase(island: Island, phase: Phase) {
        clear(island.uuid)

        island.currentPhaseId = phase.id
        island.grid = 0

        island.center.toBukkit()?.block?.let { PhaseBlocks.place(it, phase) }

        IslandHud.refresh(island)
        IslandManager.markDirty(island.uuid)
    }

    fun advance(island: Island) {
        val completed = island.getCurrentPhase() ?: return
        val members = onlineMembers(island)
        val next = PhaseManager.getNextPhase(completed.priority)

        if (next == null) {
            island.grid = completed.blocksToNextPhase
            members.forEach { it.sendMessage(Mini.deserialize(ISLAND_COMPLETE)) }
            return
        }

        if (!frozen.add(island.uuid)) return

        val material = next.blockPool.getRandomByWeight() ?: Material.STONE

        PhaseRewarder.grantOrQueue(island, completed)
        announce(completed, members)
        PhaseAnimation.start(island, material, members)

        runNextTask(PhaseAnimation.FREEZE_TICKS) {
            reveal(island, completed, next, material)
        }
    }

    private fun reveal(island: Island, completed: Phase, next: Phase, material: Material) {
        frozen.remove(island.uuid)

        island.currentPhaseId = next.id
        island.grid = 0

        island.center.toBukkit()?.block?.let { block -> PhaseBlocks.place(block, next) }

        IslandHud.refresh(island)

        PhaseAnimation.finish(island, completed, next, onlineMembers(island))
        IslandManager.markDirty(island.uuid)
    }

    private fun announce(completed: Phase, members: List<Player>) {
        if (completed.announce.isEmpty() || members.isEmpty()) return

        val lines = completed.announce.map { Mini.deserialize(it) }
        members.forEach { player -> lines.forEach { player.sendMessage(it) } }
    }

    fun onlineMembers(island: Island): List<Player> =
        Bukkit.getOnlinePlayers().filter { IslandManager.playerIsland[it.uniqueId] == island.uuid }
}
