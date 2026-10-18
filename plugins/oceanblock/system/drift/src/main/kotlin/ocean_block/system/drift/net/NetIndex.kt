package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.islands.island.IslandManager
import java.util.UUID

internal object NetIndex {

    private const val BLOCK_CENTER = 0.5

    private var byLot: Map<UUID, List<NetPoint>> = emptyMap()

    fun rebuild() {
        byLot = PlacedBlockManager.all()
            .asSequence()
            .filter { it.blockId == NetBlock.ID }
            .mapNotNull(::entry)
            .groupBy({ it.first }, { it.second })
    }

    fun forLot(islandId: UUID): List<NetPoint> = byLot[islandId].orEmpty()

    fun find(placedUuid: UUID): NetPoint? =
        byLot.values.firstNotNullOfOrNull { nets -> nets.firstOrNull { it.id == placedUuid } }

    fun refresh(placed: PlacedBlock) {
        val full = isFull(placed)
        byLot = byLot.mapValues { (_, nets) ->
            nets.map { if (it.id == placed.uuid) it.copy(full = full) else it }
        }
    }

    fun isFull(placed: PlacedBlock): Boolean =
        NetRules.isFull(placed.storageItems().map { NetSlot(it.amount, it.maxStackSize) }, NetBlock.CAPACITY)

    private fun entry(placed: PlacedBlock): Pair<UUID, NetPoint>? {
        if (!NetCatch.isLoaded(placed)) return null
        val location = placed.location.toBukkit() ?: return null
        val island = IslandManager.getIslandAt(location) ?: return null

        val point = NetPoint(
            placed.uuid,
            location.blockX + BLOCK_CENTER,
            location.blockY + BLOCK_CENTER,
            location.blockZ + BLOCK_CENTER,
            isFull(placed),
        )
        return island.uuid to point
    }
}
