package one_block.system.trader.manager

import net.kyori.adventure.text.Component
import one_block.system.islands.island.Island
import one_block.system.trader.model.ActiveTrader
import one_block.system.trader.model.TraderItem
import org.bukkit.Bukkit
import org.bukkit.entity.WanderingTrader
import java.util.*
import one_block.libs.formatter.Mini

object TraderManager {

    val activeTraders = mutableMapOf<UUID, ActiveTrader>()
    private val islandTrader = mutableMapOf<UUID, UUID>()

    private const val DESPAWN_TIME = 40

    fun spawnTrader(island: Island) {
        val spawnLoc = island.spawn.toBukkit() ?: return

        val trader = spawnLoc.world.spawn(spawnLoc.add(0.0, 2.0, 0.0), WanderingTrader::class.java) { entity ->
            entity.customName(
                Mini.mm
                    .deserialize("<gold><bold>Trader")
            )
            entity.isCustomNameVisible = true
            entity.setAI(true)
            entity.isInvulnerable = true
            entity.setCanDrinkMilk(false)
            entity.setCanDrinkPotion(false)
            entity.recipes = emptyList()
        }

        val items = ItemsRegistry.getTraderItems()
        val activeTrader = ActiveTrader(island.uuid, items, System.currentTimeMillis())

        activeTraders[trader.uniqueId] = activeTrader
        islandTrader[island.uuid] = trader.uniqueId

        island.getIslandOnlinePlayers().forEach { player ->
            val message = Component.text()
                .appendNewline()
                .append(
                    Mini.mm
                        .deserialize("  <sprite:items:item/bell> <gray>O <white>Mercador<gray> chegou em sua ilha!")
                ).appendNewline()
                .append(
                    Mini.mm.deserialize("  <white>Aproveite e compre seu estoque de produtos.")
                        .appendNewline()
                )
            player.sendMessage(message)
        }

    }

    fun hasTrader(islandId: UUID): Boolean {
        val traderId = islandTrader[islandId] ?: return false
        return Bukkit.getEntity(traderId) != null
    }

    fun getTraderItems(traderEntityId: UUID): List<TraderItem>? {
        return activeTraders[traderEntityId]?.shopItems
    }

    fun traderRemove(traderEntityId: UUID, island: Island) {
        activeTraders.remove(traderEntityId)
        islandTrader.remove(island.uuid)

        island.getIslandOnlinePlayers().forEach { player ->
            val message = Component.text()
                .appendNewline()
                .append(
                    Mini.mm
                        .deserialize("  <sprite:items:item/oak_boat> <gray>O <white>Mercador<gray> foi embora!")
                ).appendNewline()
                .append(
                    Mini.mm.deserialize("  <white>Aproveite mais da próxima vez.").appendNewline()
                )
            player.sendMessage(message)
        }

    }

}
