package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.drift.debris.Debris
import ocean_block.system.drift.debris.DebrisManager
import ocean_block.system.drift.debris.PullTarget
import org.bukkit.Location
import java.util.UUID

internal object NetCatch {

    private const val BLOCK_CENTER = 0.5
    private const val CHUNK_SHIFT = 4

    fun tryCatch(debris: Debris, radius: Double) {
        if (!debris.isCollectable) return

        val nets = NetIndex.forLot(debris.islandId).filter { PlacedBlockManager.get(it.id) != null }
        if (nets.isEmpty()) return

        val at = debris.entity.location
        val net = NetRules.nearest(at.x, at.y, at.z, nets, radius) ?: return
        DebrisManager.beginPull(debris, PullTarget.Net(net.id))
    }

    fun receiver(placedUuid: UUID): PlacedBlock? {
        val placed = PlacedBlockManager.get(placedUuid) ?: return null
        if (placed.blockId != NetBlock.ID || !isLoaded(placed)) return null

        val indexed = NetIndex.find(placedUuid) ?: return null
        return placed.takeUnless { indexed.full }
    }

    fun center(placed: PlacedBlock): Location? =
        placed.location.toBukkit()?.toBlockLocation()?.add(BLOCK_CENTER, BLOCK_CENTER, BLOCK_CENTER)

    fun isLoaded(placed: PlacedBlock): Boolean {
        val location = placed.location.toBukkit() ?: return false
        val world = location.world ?: return false
        return world.isChunkLoaded(location.blockX shr CHUNK_SHIFT, location.blockZ shr CHUNK_SHIFT)
    }
}
