package one_block.libs.item_builder

import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

inline fun itemStack(material: Material, builder: ItemStack.() -> Unit) = ItemStack(material).apply(builder)

inline fun itemStack(base: ItemStack, builder: ItemStack.() -> Unit) = base.clone().apply(builder)

inline fun <reified T : ItemMeta> ItemStack.meta(builder: T.() -> Unit) {
    val meta = itemMeta as? T
    itemMeta = if (meta != null) {
        meta.apply(builder)
        meta
    } else {
        itemMeta(type, builder)
    }
}

inline fun <reified T : ItemMeta> itemMeta(material: Material, builder: T.() -> Unit): T? {
    val meta = Bukkit.getItemFactory().getItemMeta(material)
    return if (meta is T) meta.apply(builder) else null
}

inline fun ItemMeta.setLore(builder: LoreBuilder.() -> Unit) {
    lore(LoreBuilder().apply(builder).lore)
}

fun ItemMeta.name(name: String) {
    customName(MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false))
}

inline fun ItemMeta.addLore(builder: LoreBuilder.() -> Unit) {
    val newLore = lore() ?: mutableListOf()
    newLore.addAll(LoreBuilder().apply(builder).lore)
    lore(newLore)
}

inline fun ItemMeta.nbt(builder: NbtBuilder.() -> Unit) {
    NbtBuilder(persistentDataContainer).apply(builder)
}