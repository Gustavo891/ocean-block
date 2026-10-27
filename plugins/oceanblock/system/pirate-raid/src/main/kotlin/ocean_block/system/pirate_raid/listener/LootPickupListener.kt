package ocean_block.system.pirate_raid.listener

import ocean_block.system.currencies.Currencies
import ocean_block.system.drift.collect.CollectNotifier
import ocean_block.system.drift.collect.CollectorRule
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.pirate_raid.crew.PirateKeys
import ocean_block.system.pirate_raid.raid.RaidRewards
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent

class LootPickupListener : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPickup(event: EntityPickupItemEvent) {
        val stack = event.item.itemStack
        if (!PirateKeys.isRaidLoot(stack)) return
        event.isCancelled = true
        val player = event.entity as? Player ?: return
        if (!canTake(player, event.item)) return
        val amount = stack.amount
        PirateKeys.coinValue(stack)?.let { value ->
            credit(player, RaidRewards.COINS, value * amount, "moeda_pirata", "Coins", "glyph:moeda_pirata_icone")
        }
        PirateKeys.pearlValue(stack)?.let { value ->
            credit(player, RaidRewards.PEARLS, value * amount, "perola_pirata", "Pérolas", "glyph:perola_icone")
        }
        event.item.remove()
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onHopper(event: InventoryPickupItemEvent) {
        if (PirateKeys.isRaidLoot(event.item.itemStack)) event.isCancelled = true
    }

    private fun canTake(player: Player, item: Item): Boolean {
        val owner = PirateKeys.lootIsland(item.itemStack) ?: return false
        val island = IslandManager.getIslandAt(item.location)?.takeIf { it.uuid == owner } ?: return false
        return CollectorRule.canCollect(island, player)
    }

    private fun credit(player: Player, currency: String, total: Int, key: String, label: String, icon: String) {
        Currencies.deposit(player.uniqueId, currency, total.toDouble())
        CollectNotifier.notify(player, key, label, icon, total)
    }
}
