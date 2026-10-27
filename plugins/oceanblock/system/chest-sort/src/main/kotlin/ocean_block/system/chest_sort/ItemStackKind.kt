package ocean_block.system.chest_sort

import org.bukkit.inventory.ItemStack

object ItemStackKind : StackKind<ItemStack> {
    override fun same(a: ItemStack, b: ItemStack) = a.isSimilar(b)
    override fun amount(stack: ItemStack) = if (stack.isEmpty) 0 else stack.amount
    override fun maxStack(stack: ItemStack) = stack.maxStackSize
    override fun name(stack: ItemStack) = stack.type.name
    override fun withAmount(stack: ItemStack, amount: Int): ItemStack = stack.asQuantity(amount)
}
