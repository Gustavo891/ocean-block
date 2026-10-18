package ocean_block.system.trader.manager

import ocean_block.system.islands.ext.getRandomByWeight
import ocean_block.system.trader.model.ItemRarity
import ocean_block.system.trader.model.TraderItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object ItemsRegistry {

    private val items: MutableList<TraderItem> = mutableListOf(
        TraderItem("diamante", "Diamante", ItemStack(Material.DIAMOND, 8), "coins", 20.0, 20, ItemRarity.COMMON),
        TraderItem("ouro", "Barra de Ouro", ItemStack(Material.GOLD_INGOT, 4), "coins", 10.0, 50, ItemRarity.COMMON),
        TraderItem(
            "sinalizador",
            "Sinalizador",
            ItemStack(Material.BEACON, 1),
            "coins",
            20000.0,
            2,
            ItemRarity.UNCOMMON
        ),
        TraderItem("clava", "Clava", ItemStack(Material.MACE, 1), "coins", 200000.0, 1, ItemRarity.RARE),
        TraderItem("tridente", "Tridente", ItemStack(Material.TRIDENT, 1), "coins", 60000.0, 4, ItemRarity.EPIC),
        TraderItem(
            "totem",
            "Totem da Imortalidade",
            ItemStack(Material.TOTEM_OF_UNDYING, 1),
            "coins",
            6000000.0,
            2,
            ItemRarity.LEGENDARY
        )
    )

    fun addItem(item: TraderItem) {
        items.add(item)
    }

    fun getTraderItems(): MutableList<TraderItem> {
        val remaining = items.toMutableList()
        val selected = mutableListOf<TraderItem>()

        repeat(6) {
            if (remaining.isEmpty()) return@repeat
            val pick = remaining.associateWith { it.rarity.weight }.getRandomByWeight() ?: return@repeat
            selected.add(pick.copy())
            remaining.remove(pick)
        }

        return selected
    }
}
