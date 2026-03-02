package one_block.system.sell.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.CloseContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import one_block.libs.formatter.NumberFormatter
import one_block.libs.instance.Instance
import one_block.system.currencies.Currencies
import one_block.system.sell.manager.SellManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class SellView : View() {

    companion object {
        val SELL_SLOTS = (0..53).toList()
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.title(Component.text("Venda seus itens"))
            .size(6)
    }

    override fun onClick(click: SlotClickContext) {
        val player = click.player
        Bukkit.getScheduler().runTaskLaterAsynchronously(Instance, Runnable {
            val total = calcTotal(click.parent.inventory)
            val title = if (total > 0.0) {
                println("Total: R$${NumberFormatter.formatNumber(total)}")
                "§8Venda de Itens > §2R$§f${NumberFormatter.formatNumber(total)}"
            } else
                "<dark_gray>Venda de Itens"
            click.updateTitleForPlayer(title, player)
        }, 1L)
    }

    override fun onClose(close: CloseContext) {
        val player = close.player as? Player ?: return
        val viewInventory = close.parent.inventory


        var totalMoney = 0.0
        val unsellable = mutableListOf<ItemStack>()

        SELL_SLOTS.forEach { slot ->
            val item = viewInventory.getItem(slot) ?: return@forEach
            if (item.type == Material.AIR) return@forEach

            val price = SellManager.getPrice(item.type)
            if (price <= 0.0) {
                unsellable.add(item.clone())
            } else {
                totalMoney += price * item.amount
            }

            viewInventory.setItem(slot, null)
        }

        unsellable.forEach { item ->
            val leftover = player.inventory.addItem(item)
            leftover.values.forEach { dropped ->
                player.world.dropItemNaturally(player.location, dropped)
            }
        }

        if (totalMoney <= 0.0) {
            if (unsellable.isNotEmpty()) {
                player.sendRichMessage("<color:#b00f00>Nenhum item vendável encontrado.")
            }
            return
        }

        Currencies.deposit(player.uniqueId, "coins", totalMoney)
        player.sendRichMessage("<green>Você recebeu <dark_green>$<white>${"%.2f".format(totalMoney)}</white> <green>com suas vendas!")
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
    }

    private fun calcTotal(inventory: Inventory): Double {
        return SELL_SLOTS.sumOf { slot ->
            val item = inventory.getItem(slot) ?: return@sumOf 0.0
            if (item.type == Material.AIR) return@sumOf 0.0
            SellManager.getPrice(item.type) * item.amount
        }
    }
}