package one_block.trader.model

import one_block.system.currencies.Currencies
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class TraderItem(
    val id: String,
    val prettyName: String,
    val displayItem: ItemStack,
    val currency: String,
    val price: Double,
    var stock: Int,
    val rarity: ItemRarity,
) {
    fun hasStock() = stock > 0

    fun buy(player: Player): Boolean {
        if (!hasStock()) return false

        if (Currencies.has(player.uniqueId, currency, price)) {
            stock--
            Currencies.withdraw(player.uniqueId, currency, price)
            val item = displayItem.clone()
            player.inventory.addItem(item)
            player.sendRichMessage("<yellow>Você adquiriu <white>${prettyName}<yellow> do mercador!")
            return true
        } else {
            return false
        }
    }
}
