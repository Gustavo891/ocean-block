package one_block.system.custom_items.item

import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.item.context.ConsumeContext
import one_block.system.custom_items.item.context.EntityContext
import one_block.system.custom_items.item.context.InteractContext
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

abstract class CustomItem {

    abstract val id: String
    abstract val material: Material
    open val itemModel: NamespacedKey? = null
    open val displayName: String? = null
    open val lore: List<String> = emptyList()
    open val cooldownMillis: Long = 0L
    open val heldTickInterval: Long = 5L

    open fun build(amount: Int = 1): ItemStack = itemStack(material) {
        this.amount = amount
        meta<ItemMeta> {
            this@CustomItem.itemModel?.let { model -> itemModel = model }
            this@CustomItem.displayName?.let { dn -> name(dn) }
            this@CustomItem.lore.takeIf { it.isNotEmpty() }?.let { loreList ->
                setLore { loreList.forEach { +it } }
            }
            nbt { string(CustomItemKeys.ID, this@CustomItem.id) }
        }
    }

    open fun onRightClick(ctx: InteractContext) {}
    open fun onLeftClick(ctx: InteractContext) {}
    open fun onInteractEntity(ctx: EntityContext) {}
    open fun onConsume(ctx: ConsumeContext) {}
    open fun onHeldTick(player: Player, item: ItemStack) {}

    open fun listeners(): List<Listener> = emptyList()

    open fun tasks(): List<CustomItemTask> = emptyList()
}
