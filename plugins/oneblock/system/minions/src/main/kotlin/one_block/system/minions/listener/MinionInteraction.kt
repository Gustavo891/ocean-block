package one_block.system.minions.listener

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.minions.Minions
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.system.minions.view.MinionView
import org.bukkit.entity.Player

object MinionInteraction {

    fun handle(player: Player, placedMinion: PlacedMinion, viewFrame: ViewFrame) {
        val item = player.inventory.itemInMainHand

        val fuelAmount = Minions.getFuelAmount(item)
        if (fuelAmount > 0) {
            placedMinion.addEnergy(fuelAmount)
            PlacedMinionManager.update(placedMinion)

            item.amount -= 1

            player.sendMessage("Você adicionou $fuelAmount de energia ao minion. Energia atual: ${placedMinion.energy}")
            return
        }

        viewFrame.open(MinionView::class.java, player, placedMinion)
    }
}
