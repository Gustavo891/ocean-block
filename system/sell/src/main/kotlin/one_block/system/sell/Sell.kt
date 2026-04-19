package one_block.system.sell

import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.instance.Instance
import one_block.system.currencies.Currencies
import one_block.system.sell.listener.SellEvent
import one_block.system.sell.manager.SellManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object Sell {

    private val ITEM_SELL_KEY = NamespacedKey(Instance, "sellitem")

    fun createSellItem(uses: Int): ItemStack {
        val itemStack = ItemStack(Material.STICK)
        val itemMeta = itemStack.itemMeta

        val persistentDataContainer = itemMeta.persistentDataContainer
        persistentDataContainer.set(ITEM_SELL_KEY, PersistentDataType.INTEGER, uses)
        itemStack.itemMeta = itemMeta
        c(itemStack, uses)

        return itemStack
    }

    fun getUsesByItem(itemStack: ItemStack) = itemStack.itemMeta?.persistentDataContainer?.get(
        ITEM_SELL_KEY, PersistentDataType.INTEGER
    )

    fun updateUsesByItem(itemStack: ItemStack, uses: Int) {
        if (uses <= 0) {
            itemStack.amount = 0
            return
        }

        itemStack.editMeta {
            val persistentDataContainer = it.persistentDataContainer
            persistentDataContainer.set(ITEM_SELL_KEY, PersistentDataType.INTEGER, uses)
        }

        c(itemStack, uses)
    }

    fun sellItem(player: Player, itemStack: ItemStack?): Double {
        if (itemStack == null) return 0.0
        val price = SellManager.getPrice(itemStack.type)

        if (price == 0.0) return 0.0
        val total = price * itemStack.amount

        return total
    }

    fun sellContainer(player: Player, block: Block): Double {
        val state = block.state
        if (state !is Chest) return 0.0

        val inventory = state.inventory
        var totalMoney = 0.0

        inventory.contents.filterNotNull().forEach {
            val sellPrice = sellItem(player, it)
            if (sellPrice == 0.0) return@forEach

            val sellEvent = SellEvent(player, it.clone(), sellPrice)
            SellSystem.plugin.server.pluginManager.callEvent(sellEvent)

            totalMoney += sellPrice
            it.amount = 0
        }

        if (totalMoney > 0.0) {
            Currencies.deposit(player.uniqueId, "coins", totalMoney)
        }

        return totalMoney
    }

    fun c(itemStack: ItemStack, uses: Int) {
        itemStack.editMeta {
            it.customName(MiniMessage.miniMessage().deserialize("<yellow>Vendedor"))

            it.lore(
                listOf(
                    MiniMessage.miniMessage().deserialize("<white>Usos: <b>$uses</b>"),
                    MiniMessage.miniMessage().deserialize(""),
                    MiniMessage.miniMessage().deserialize("<white>Bata em um baú"),
                    MiniMessage.miniMessage().deserialize("<white>para vender.")
                )
            )
        }
    }
}