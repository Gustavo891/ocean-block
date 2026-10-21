package ocean_block.system.stations.sieve

import ocean_block.system.drift.collect.CollectNotifier
import ocean_block.system.stations.item.OrePiece
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object SieveDelivery {

    fun stackOf(output: String, amount: Int): ItemStack? =
        OrePiece.byId(output)?.build(amount) ?: Material.matchMaterial(output)?.let { ItemStack(it, amount) }

    fun deliver(player: Player, outputs: List<Pair<String, Int>>) {
        outputs.forEach { (output, amount) ->
            val stack = stackOf(output, amount) ?: return@forEach
            player.inventory.addItem(stack).values.forEach { player.world.dropItemNaturally(player.location, it) }
            notify(player, output, amount)
        }
        if (outputs.isNotEmpty()) player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f)
    }

    fun icons(outputs: Set<String>): List<String> =
        outputs.mapNotNull { output -> OrePiece.byId(output)?.material ?: Material.matchMaterial(output) }
            .map { it.key.key }
            .distinct()

    private fun notify(player: Player, output: String, amount: Int) {
        val piece = OrePiece.byId(output)
        if (piece != null) {
            CollectNotifier.notify(player, piece.id, piece.label, CollectNotifier.iconOf(piece.material), amount)
            return
        }
        Material.matchMaterial(output)?.let { CollectNotifier.notify(player, it, amount) }
    }
}
