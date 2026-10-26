package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.islands.island.IslandManager
import one_block.textura.api.NotificationApi
import one_block.textura.notify.NotificationLine
import org.bukkit.entity.Player

internal object NetFullNotifier {

    private const val REGION = "avisos"
    private const val DURATION_TICKS = 100
    private const val TEXT = "<yellow>Sua Rede de Detritos ficou cheia!</yellow>"

    const val ICON = "glyph:rede_icone"

    fun afterChange(placed: PlacedBlock) {
        val full = NetIndex.isFull(placed)
        if (full == placed.notifiedFull) return

        placed.notifiedFull = full
        PlacedBlockManager.markDirty(placed)
        if (full) announce(placed)
    }

    fun preview(player: Player) {
        NotificationApi.push(player, REGION, NotificationLine(TEXT, ICON, DURATION_TICKS, "rede-cheia:exemplo"))
    }

    private fun announce(placed: PlacedBlock) {
        val location = placed.location.toBukkit() ?: return
        val island = IslandManager.getIslandAt(location) ?: return
        val line = NotificationLine(TEXT, ICON, DURATION_TICKS, "rede-cheia:${placed.uuid}")
        island.getIslandOnlinePlayers().forEach { NotificationApi.push(it, REGION, line) }
    }
}
