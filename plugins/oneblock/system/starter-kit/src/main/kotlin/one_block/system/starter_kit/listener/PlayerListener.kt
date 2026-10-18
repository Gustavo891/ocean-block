package one_block.system.starter_kit.listener

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

class PlayerListener : Listener {

    companion object {
        private val SWORD_KEY = NamespacedKey("nexo", "forest_sword")
        private val AXE_KEY = NamespacedKey("nexo", "forest_axe")
        private val PICKAXE_KEY = NamespacedKey("nexo", "forest_pickaxe")
        private val SHOVEL_KEY = NamespacedKey("nexo", "forest_shovel")
        private val HOE_KEY = NamespacedKey("nexo", "forest_hoe")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPlayedBefore()) return

        val inventory = player.inventory
        inventory.addItem(createItemStack(Material.STONE_SWORD, SWORD_KEY))
        inventory.addItem(createItemStack(Material.STONE_PICKAXE, PICKAXE_KEY))
        inventory.addItem(createItemStack(Material.STONE_AXE, AXE_KEY))
        inventory.addItem(createItemStack(Material.STONE_SHOVEL, SHOVEL_KEY))
        inventory.addItem(createItemStack(Material.STONE_HOE, HOE_KEY))

        inventory.addItem(ItemStack(Material.COOKED_BEEF, 8))
    }

    private fun createItemStack(material: Material, key: NamespacedKey): ItemStack {
        val itemStack = ItemStack(material)
        return itemStack
    }
}
