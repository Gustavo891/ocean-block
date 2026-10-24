package ocean_block.system.machines

import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.custom_items.CustomIds
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.islands.island.IslandManager
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object MachineItems {

    fun stackOf(material: String, amount: Int): ItemStack? =
        Material.matchMaterial(material)?.takeIf { amount > 0 }?.let { ItemStack(it, amount) }

    fun isVanilla(item: ItemStack?): Boolean = item != null && !item.type.isAir && CustomIds.of(item) == null

    fun give(player: Player, items: List<Pair<String, Int>>) {
        val stacks = items.mapNotNull { (material, amount) -> stackOf(material, amount) }
        stacks.forEach { stack ->
            player.inventory.addItem(stack).values.forEach { player.world.dropItemNaturally(player.location, it) }
        }
        if (stacks.isNotEmpty()) player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1f)
    }

    fun canUse(player: Player, placed: PlacedBlock): Boolean {
        if (!PlacedBlockManager.isRegistered(placed)) return false
        val island = placed.location.toBukkit()?.let(IslandManager::getIslandAt) ?: return false
        return island.ownerUuid == player.uniqueId || island.isMember(player.name)
    }

    fun energyLine(stored: Long, capacity: Long): String =
        "<gray>Energia: <yellow>${NumberFormatter.format(stored.toDouble())}<gray>/${NumberFormatter.format(capacity.toDouble())} E"

    fun takeFromHand(player: Player, amount: Int) {
        val hand = player.inventory.itemInMainHand
        hand.amount -= amount
        player.inventory.setItemInMainHand(hand.takeIf { it.amount > 0 })
    }
}
