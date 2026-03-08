package one_block.trader.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.system.islands.island.Island
import one_block.trader.model.ActiveTrader
import one_block.trader.model.TraderItem
import org.bukkit.Bukkit
import org.bukkit.entity.WanderingTrader
import java.util.*

object TraderManager {

    val activeTraders = mutableMapOf<UUID, ActiveTrader>()
    private val islandTrader = mutableMapOf<UUID, UUID>()

    private const val DESPAWN_TIME = 40 // em minutos

    fun spawnTrader(island: Island) {
        val spawnLoc = island.spawn.toBukkit() ?: return

        val trader = spawnLoc.world.spawn(spawnLoc.add(0.0, 2.0, 0.0), WanderingTrader::class.java) { entity ->
            entity.customName(
                MiniMessage.miniMessage()
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
                    MiniMessage.miniMessage()
                        .deserialize("  <sprite:items:item/bell> <gray>O <white>Mercador<gray> chegou em sua ilha!")
                ).appendNewline()
                .append(
                    MiniMessage.miniMessage().deserialize("  <white>Aproveite e compre seu estoque de produtos.")
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
                    MiniMessage.miniMessage()
                        .deserialize("  <sprite:items:item/oak_boat> <gray>O <white>Mercador<gray> foi embora!")
                ).appendNewline()
                .append(
                    MiniMessage.miniMessage().deserialize("  <white>Aproveite mais da próxima vez.").appendNewline()
                )
            player.sendMessage(message)
        }

    }

}