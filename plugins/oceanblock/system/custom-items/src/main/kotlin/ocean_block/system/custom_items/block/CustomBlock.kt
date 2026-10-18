package ocean_block.system.custom_items.block

import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.nbt
import ocean_block.libs.item_builder.setLore
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.custom_items.block.context.BreakContext
import ocean_block.system.custom_items.block.context.PlaceContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

abstract class CustomBlock {

    abstract val id: String
    abstract val material: Material
    abstract val displayName: String
    open val lore: List<String> = emptyList()
    open val itemModel: NamespacedKey? = null

    open val limitPerIsland: Int = 5

    open val upgrades: List<BlockUpgrade> = emptyList()

    open fun storageCapacity(placed: PlacedBlock): Int = 0

    open fun tickInterval(placed: PlacedBlock): Long = 20L

    open fun buildItem(placed: PlacedBlock? = null, amount: Int = 1): ItemStack = itemStack(material) {
        this.amount = amount
        meta<ItemMeta> {
            this@CustomBlock.itemModel?.let { model -> itemModel = model }
            name(this@CustomBlock.displayName)

            setLore {
                this@CustomBlock.lore.forEach { +it }
                if (placed != null) {
                    val evolved = this@CustomBlock.upgrades.filter { placed.getUpgradeLevel(it) > 0 }
                    if (evolved.isNotEmpty()) {
                        +""
                        evolved.forEach { upgrade ->
                            +"<dark_gray>▸ <white>${upgrade.prettyName}: ${upgrade.statLine(placed.getUpgradeLevel(upgrade))}"
                        }
                    }
                    val storedCount = placed.storageItems().size
                    if (storedCount > 0) {
                        +"<dark_gray>▸ <white>Itens guardados: <aqua>$storedCount"
                    }
                }
            }

            nbt { string(CustomBlockKeys.ID, this@CustomBlock.id) }
            placed?.let { PlacedBlockCodec.write(this, it) }
        }
    }

    open fun onPlace(ctx: PlaceContext) {}

    open fun onBreak(ctx: BreakContext) {}

    open fun onRightClick(ctx: BlockClickContext) {}

    open fun tick(placed: PlacedBlock) {}
}
