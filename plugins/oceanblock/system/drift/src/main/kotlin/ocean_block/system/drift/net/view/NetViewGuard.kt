package ocean_block.system.drift.net.view

import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.islands.island.IslandManager

internal fun SlotClickContext.requireAccess(placed: PlacedBlock): Boolean {
    if (!PlacedBlockManager.isRegistered(placed)) {
        player.sendRichMessage("<red>Esta rede não existe mais.")
        closeForPlayer()
        return false
    }

    val island = placed.location.toBukkit()?.let(IslandManager::getIslandAt)
    if (island != null && CollectorRule.canCollect(island, player)) return true

    closeForPlayer()
    return false
}
