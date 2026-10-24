package ocean_block.system.sell

import ocean_block.libs.instance.Instance
import ocean_block.system.currencies.Currencies
import ocean_block.system.sell.listener.SellEvent
import ocean_block.system.sell.manager.SellManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import ocean_block.libs.formatter.Mini

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

    fun sellInventory(player: Player): SaleSummary {
        val inventory = player.inventory
        var totalMoney = 0.0
        var amount = 0

        inventory.storageContents.forEachIndexed { slot, itemStack ->
            if (itemStack == null || itemStack.type == Material.AIR) return@forEachIndexed
            if (getUsesByItem(itemStack) != null) return@forEachIndexed

            val sellPrice = sellItem(player, itemStack)
            if (sellPrice <= 0.0) return@forEachIndexed

            val sellEvent = SellEvent(player, itemStack.clone(), sellPrice)
            SellSystem.plugin.server.pluginManager.callEvent(sellEvent)

            totalMoney += sellPrice
            amount += itemStack.amount
            inventory.setItem(slot, null)
        }

        if (totalMoney > 0.0) {
            Currencies.deposit(player.uniqueId, "coins", totalMoney)
        }

        return SaleSummary(totalMoney, amount)
    }

    fun sellContainer(player: Player, block: Block): SaleSummary {
        val state = block.state
        if (state !is Chest) return SaleSummary(0.0, 0)

        val inventory = state.inventory
        var totalMoney = 0.0
        var amount = 0

        inventory.contents.filterNotNull().forEach {
            val sellPrice = sellItem(player, it)
            if (sellPrice == 0.0) return@forEach

            val sellEvent = SellEvent(player, it.clone(), sellPrice)
            SellSystem.plugin.server.pluginManager.callEvent(sellEvent)

            totalMoney += sellPrice
            amount += it.amount
            it.amount = 0
        }

        if (totalMoney > 0.0) {
            Currencies.deposit(player.uniqueId, "coins", totalMoney)
        }

        return SaleSummary(totalMoney, amount)
    }

    fun c(itemStack: ItemStack, uses: Int) {
        itemStack.editMeta {
            it.customName(Mini.mm.deserialize("<yellow>Vendedor"))

            it.lore(
                listOf(
                    Mini.mm.deserialize("<white>Usos: <b>$uses</b>"),
                    Mini.mm.deserialize(""),
                    Mini.mm.deserialize("<white>Bata em um baú"),
                    Mini.mm.deserialize("<white>para vender.")
                )
            )
        }
    }
}
