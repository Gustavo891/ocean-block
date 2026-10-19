package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.islands.island.IslandManager
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.Material

internal object NetFullNotifier {

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 100
    private const val TEXT = "<yellow>Sua Rede de Detritos ficou cheia!</yellow>"

    val ICON = "item:${Material.LOOM.key.key}"

    fun afterChange(placed: PlacedBlock) {
        val full = NetIndex.isFull(placed)
        if (full == placed.notifiedFull) return

        placed.notifiedFull = full
        PlacedBlockManager.markDirty(placed)
        if (full) announce(placed)
    }

    private fun announce(placed: PlacedBlock) {
        val location = placed.location.toBukkit() ?: return
        val island = IslandManager.getIslandAt(location) ?: return
        val line = NotificationLine(TEXT, ICON, DURATION_TICKS, "rede-cheia:${placed.uuid}")
        island.getIslandOnlinePlayers().forEach { NotificationApi.push(it, REGION, line) }
    }
}
