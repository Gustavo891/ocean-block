package ocean_block.system.drift.hook

import ocean_block.system.custom_items.item.CustomItem
import org.bukkit.Material
import org.bukkit.event.Listener

object GanchoItem : CustomItem() {

    override val id = "gancho"
    override val material = Material.FISHING_ROD
    override val displayName = "<white>Gancho"
    override val lore = listOf(
        "<gray>Lance na água e recolha",
        "<gray>para puxar detritos.",
    )

    override fun listeners(): List<Listener> = listOf(GanchoListener())
}
