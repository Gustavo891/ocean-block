package ocean_block.system.drift.collect

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object DebrisDelivery {

    private const val SPLASH_COUNT = 20

    fun deliver(player: Player, drops: List<Pair<Material, Int>>) {
        if (drops.isEmpty()) return

        drops.forEach { (material, amount) ->
            give(player, ItemStack(material, amount))
            CollectNotifier.notify(player, material, amount)
        }

        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
        player.world.spawnParticle(Particle.SPLASH, player.location, SPLASH_COUNT)
    }

    private fun give(player: Player, stack: ItemStack) {
        player.inventory.addItem(stack).values.forEach { player.world.dropItem(player.location, it) }
    }
}
