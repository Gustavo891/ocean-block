package one_block.libs.item_builder

import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta

object Items {

    fun RenderContext.createEmptyRender(slot: Char, block: SlotClickContext.() -> Unit) {
        layoutSlot(slot)
            .renderWith(::createEmptyItem)
            .onClick { click -> block(click) }
    }

    private fun createEmptyItem() = itemStack(Material.PAPER) {
        meta<ItemMeta> {
            itemModel = NamespacedKey("oneblock", "blank")
            isHideTooltip = true
        }
    }
}