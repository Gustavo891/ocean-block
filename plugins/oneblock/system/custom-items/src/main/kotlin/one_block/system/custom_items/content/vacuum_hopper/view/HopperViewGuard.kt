package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager

internal fun SlotClickContext.requireAlive(placed: PlacedBlock): Boolean {
    if (PlacedBlockManager.isRegistered(placed)) return true

    player.sendRichMessage("<red>Este bloco não existe mais.")
    closeForPlayer()
    return false
}
