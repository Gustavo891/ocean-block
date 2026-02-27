package one_block.system.starter_kit.listener

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

class PlayerListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPlayedBefore()) return

        val inventory = player.inventory
        inventory.addItem(ItemStack(Material.WOODEN_SWORD))
        inventory.addItem(ItemStack(Material.WOODEN_PICKAXE))
        inventory.addItem(ItemStack(Material.WOODEN_AXE))
        inventory.addItem(ItemStack(Material.WOODEN_SHOVEL))
        inventory.addItem(ItemStack(Material.WOODEN_HOE))

        inventory.addItem(ItemStack(Material.COOKED_BEEF, 8))
    }
}