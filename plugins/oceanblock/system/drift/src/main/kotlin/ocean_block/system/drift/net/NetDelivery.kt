package ocean_block.system.drift.net

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.inventory.ItemStack

internal object NetDelivery {

    private const val SPLASH_COUNT = 20

    fun deliver(placed: PlacedBlock, drops: List<Pair<Material, Int>>, at: Location) {
        if (drops.isEmpty()) return

        val inventory = placed.buildStorageInventory(NetBlock.CAPACITY)
        drops.forEach { (material, amount) -> inventory.addItem(ItemStack(material, amount)) }

        placed.setStorage(inventory)
        PlacedBlockManager.markDirty(placed)
        NetIndex.refresh(placed)
        at.world?.spawnParticle(Particle.SPLASH, at, SPLASH_COUNT)
    }
}
