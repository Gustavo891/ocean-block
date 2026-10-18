package ocean_block.system.drift.collect

import net.kyori.adventure.text.Component
import ocean_block.libs.actionbar.ActionBar
import ocean_block.libs.actionbar.Channel
import ocean_block.libs.formatter.Mini
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object DebrisDelivery {

    private const val SPLASH_COUNT = 20
    private const val ACTION_BAR_TICKS = 40
    private const val SEPARATOR = "<gray>  "

    fun deliver(player: Player, drops: List<Pair<Material, Int>>) {
        if (drops.isEmpty()) return

        drops.forEach { (material, amount) -> give(player, ItemStack(material, amount)) }

        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
        player.world.spawnParticle(Particle.SPLASH, player.location, SPLASH_COUNT)
        ActionBar.send(player, Channel.Drift, summary(drops), ACTION_BAR_TICKS)
    }

    private fun give(player: Player, stack: ItemStack) {
        player.inventory.addItem(stack).values.forEach { player.world.dropItem(player.location, it) }
    }

    private fun summary(drops: List<Pair<Material, Int>>): Component =
        Mini.deserialize(
            drops.joinToString(SEPARATOR) { (material, amount) ->
                "<green>+$amount <lang:${material.translationKey()}>"
            },
        )
}
