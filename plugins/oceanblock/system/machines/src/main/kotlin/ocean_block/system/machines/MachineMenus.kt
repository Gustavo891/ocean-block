package ocean_block.system.machines

import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.custom_items.block.PlacedBlock
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack

object MachineMenus {

    const val UPDATE_TICKS = 10L

    fun title(text: String): Component = Mini.mm.deserialize("<dark_gray>$text")

    fun icon(material: Material, title: String, lore: List<String>): ItemStack = itemStack(material) {
        meta {
            name(title)
            setLore { lore.forEach { +it } }
        }
    }

    fun requireAccess(click: SlotClickContext, placed: PlacedBlock): Boolean {
        if (MachineItems.canUse(click.player, placed)) return true
        click.player.sendRichMessage("<red>Esta máquina não está mais disponível.")
        click.closeForPlayer()
        return false
    }

    fun depositFromInventory(click: SlotClickContext, placed: PlacedBlock, insert: (ItemStack) -> Int): Boolean {
        if (!click.isOnEntityContainer) return false
        click.isCancelled = true
        if (!requireAccess(click, placed)) return true
        val event = click.clickOrigin
        val stack = event.currentItem ?: return true
        if (!MachineItems.isVanilla(stack)) return true
        val taken = insert(stack.clone())
        if (taken <= 0) {
            click.player.playSound(click.player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            return true
        }
        stack.amount -= taken
        event.currentItem = stack.takeIf { it.amount > 0 }
        click.player.playSound(click.player, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f)
        click.update()
        return true
    }
}
