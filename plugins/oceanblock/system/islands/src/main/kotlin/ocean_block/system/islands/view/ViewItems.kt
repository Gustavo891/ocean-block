package ocean_block.system.islands.view

import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta

internal fun backItem() = navItem("<red>Voltar")

internal fun navItem(label: String) = itemStack(Material.ARROW) {
    meta<ItemMeta> {
        name(label)
    }
}

internal fun menuTitle(title: String) = Mini.mm.deserialize("<dark_gray>$title")

internal fun headOf(player: Player, build: ItemMeta.() -> Unit): ItemStack = itemStack(Material.PLAYER_HEAD) {
    meta<SkullMeta> {
        owningPlayer = player
        addItemFlags(*ItemFlag.entries.toTypedArray())
        build()
    }
}

internal fun headOf(playerName: String, build: ItemMeta.() -> Unit): ItemStack = itemStack(Material.PLAYER_HEAD) {
    meta<SkullMeta> {
        val cached = Bukkit.getPlayerExact(playerName) ?: Bukkit.getOfflinePlayerIfCached(playerName)
        if (cached != null) owningPlayer = cached else playerProfile = Bukkit.createProfile(null, playerName)
        addItemFlags(*ItemFlag.entries.toTypedArray())
        build()
    }
}
