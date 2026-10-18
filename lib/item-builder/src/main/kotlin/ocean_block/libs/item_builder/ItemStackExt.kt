package ocean_block.libs.item_builder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import ocean_block.libs.formatter.Mini
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

inline fun itemStack(material: Material, builder: ItemStack.() -> Unit) = ItemStack(material).apply(builder)

inline fun itemStack(base: ItemStack, builder: ItemStack.() -> Unit) = base.clone().apply(builder)

@JvmName("metaTyped")
inline fun <reified T : ItemMeta> ItemStack.meta(builder: T.() -> Unit) {
    val meta = itemMeta as? T
    itemMeta = if (meta != null) {
        meta.apply(builder)
        meta
    } else {
        itemMeta(type, builder)
    }
}

inline fun ItemStack.meta(builder: ItemMeta.() -> Unit) {
    val meta = itemMeta ?: return
    meta.apply(builder)
    itemMeta = meta
}

inline fun <reified T : ItemMeta> itemMeta(material: Material, builder: T.() -> Unit): T? {
    val meta = Bukkit.getItemFactory().getItemMeta(material)
    return if (meta is T) meta.apply(builder) else null
}

inline fun ItemMeta.setLore(builder: LoreBuilder.() -> Unit) {
    lore(LoreBuilder().apply(builder).lore)
}

fun ItemMeta.name(name: String) {
    customName(Mini.deserialize(name).decoration(TextDecoration.ITALIC, false))
}

fun ItemMeta.name(name: Component) {
    customName(name)
}

inline fun ItemMeta.addLore(builder: LoreBuilder.() -> Unit) {
    val newLore = lore() ?: mutableListOf()
    newLore.addAll(LoreBuilder().apply(builder).lore)
    lore(newLore)
}

inline fun ItemMeta.nbt(builder: NbtBuilder.() -> Unit) {
    NbtBuilder(persistentDataContainer).apply(builder)
}


fun ItemStack.getNbtInt(key: NamespacedKey, def: Int = 0): Int {
    return itemMeta?.persistentDataContainer?.get(key, PersistentDataType.INTEGER) ?: def
}

fun ItemStack.getNbtString(key: NamespacedKey, def: String = ""): String {
    return itemMeta?.persistentDataContainer?.get(key, PersistentDataType.STRING) ?: def
}

fun ItemStack.getNbtDouble(key: NamespacedKey, def: Double = 0.0): Double {
    return itemMeta?.persistentDataContainer?.get(key, PersistentDataType.DOUBLE) ?: def
}

fun ItemStack.getNbtLong(key: NamespacedKey, def: Long = 0L): Long {
    return itemMeta?.persistentDataContainer?.get(key, PersistentDataType.LONG) ?: def
}

fun ItemStack.getNbtBoolean(key: NamespacedKey, def: Boolean = false): Boolean {
    return itemMeta?.persistentDataContainer?.get(key, PersistentDataType.BOOLEAN) ?: def
}

