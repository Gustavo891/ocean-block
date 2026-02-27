package one_block.system.sell

import one_block.libs.instance.Instance
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
        return itemStack
    }

    fun getUsesByItem(itemStack: ItemStack) = itemStack.itemMeta?.persistentDataContainer?.get(
        ITEM_SELL_KEY, PersistentDataType.INTEGER
    )

    fun updateUsesByItem(itemStack: ItemStack, uses: Int) {
        itemStack.editMeta {
            val persistentDataContainer = it.persistentDataContainer
            persistentDataContainer.set(ITEM_SELL_KEY, PersistentDataType.INTEGER, uses)
        }
    }

    fun sellItem(player: Player, itemStack: ItemStack?): Double {
        if (itemStack == null) return 0.0
        val price = SellManager.getPrice(itemStack.type)

        if (price == 0.0) return 0.0
        val total = price * itemStack.amount

        // manda o coins pro amigo
        return total
    }

    fun sellHand(player: Player): Double {
        val itemInMainHand = player.inventory.itemInMainHand
        val total = sellItem(player, itemInMainHand)
        if (total > 0) itemInMainHand.amount = 0

        return total
    }

    fun sellContainer(player: Player, block: Block): Double {
        val state = block.state
        if (state !is Chest) return 0.0

        val inventory = state.inventory
        var totalMoney = 0.0

        inventory.contents.filterNotNull().forEach {
            val sellItem = sellItem(player, it)
            if (sellItem == 0.0) return@forEach

            totalMoney += sellItem
            inventory.removeItem(ItemStack(it.type, it.amount))
        }

        return totalMoney
    }
}