package ocean_block.system.islands.view

import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta

internal fun backItem() = itemStack(Material.ARROW) {
    meta<ItemMeta> {
        name("<red>Voltar")
    }
}
