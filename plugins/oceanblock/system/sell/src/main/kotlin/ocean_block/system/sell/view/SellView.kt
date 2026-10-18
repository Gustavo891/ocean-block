package ocean_block.system.sell.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.CloseContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import net.kyori.adventure.text.Component
import ocean_block.libs.formatter.Mini
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.instance.Instance
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.system.currencies.Currencies
import ocean_block.system.sell.SellSystem
import ocean_block.system.sell.listener.SellEvent
import ocean_block.system.sell.manager.SellManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.BooleanSupplier

class SellView : View() {

    companion object {
        val SELL_SLOTS = (0..26).toList()
        val LINE_SLOTS = listOf(
            (1..7).toList(),
            (10..16).toList(),
            (19..25).toList()
        )

        fun getLineAmount(player: Player): Int = when {
            player.hasPermission("sell.line.3") -> 3
            player.hasPermission("sell.line.2") -> 2
            else -> 1
        }

        fun getSellSlots(player: Player): List<Int> =
            LINE_SLOTS.take(getLineAmount(player)).flatten()

        fun getLockedSlots(player: Player): List<Int> =
            LINE_SLOTS.drop(getLineAmount(player)).flatten()

        val BORDER_SLOTS = setOf(0, 8, 9, 17, 18, 26, 27, 28, 29, 30, 32, 33, 34, 35)
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:sell_menu><font:oneblock:gui><shift:-170><dark_gray>Venda")
        )
            .layout(
                "V       V",
                "V       V",
                "V       V",
                "VVVVSVVVV"
            )

    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('V')
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        isHideTooltip = true
                        itemModel = NamespacedKey("oneblock", "blank")
                    }
                }
            }
        render.layoutSlot('S')
            .renderWith {
                itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        itemModel = NamespacedKey("oneblock", "blank")
                        displayName(Component.text("§aConfirmar"))
                        addLore {
                            +"<gray>Você está vendendo seus"
                            +"<gray>itens por <dark_green>R$<white>${calcTotal(render.inventory)}<gray>."
                            +""
                            +"<green>Clique para vender!"
                        }
                    }
                }
            }
            .displayIf(BooleanSupplier { calcTotal(render.inventory) > 0.0 } )
            .onClick { click ->
                click.isCancelled = true
                click.closeForPlayer()
                sellItems(click.parent.inventory, click.player)
            }

        getLockedSlots(render.player).forEach { slot ->
            render.slot(slot)
                .renderWith { itemStack(Material.PAPER) {
                    meta<ItemMeta> {
                        displayName(Component.text("§cBloqueado"))
                        addLore {
                            + "<gray>Adquira seu <white>VIP<gray> em"
                            + "<white>loja.redescreen.com"
                            + "<gray>para liberar mais slots."
                        }
                        itemModel = NamespacedKey("oneblock", "locked")
                    }
                } }
        }
    }

    override fun onClick(click: SlotClickContext) {
        val player = click.player
        val slot = click.slot

        if(slot in BORDER_SLOTS || slot in getLockedSlots(player)) {
            click.isCancelled = true
            return
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(Instance, Runnable {
            val total = calcTotal(click.parent.inventory)
            val title = if (total > 0.0) {
                "<white><shift:-8><glyph:sell_menu><font:oneblock:gui><shift:-170><dark_gray>Venda > R$${
                    NumberFormatter.formatNumber(
                        total
                    )
                }"
            } else
                "<white><shift:-8><glyph:sell_menu><font:oneblock:gui><shift:-170><dark_gray>Venda"
            click.updateTitleForPlayer(Mini.mm.deserialize(title))
            click.parent.update()
        }, 1L)
    }

    override fun onClose(close: CloseContext) {
        val player = close.player
        val viewInventory = close.parent.inventory

        sellItems(viewInventory, player)
    }

    private fun sellItems(viewInventory: Inventory, player: Player) {
        var totalMoney = 0.0
        val unsellable = mutableListOf<ItemStack>()
        val sellSlots = getSellSlots(player)

        sellSlots.forEach { slot ->
            val item = viewInventory.getItem(slot) ?: return@forEach
            if (item.type == Material.AIR) return@forEach

            if(item.itemMeta.itemModel == NamespacedKey("oneblock", "blank")) return@forEach
            val price = SellManager.getPrice(item.type)
            if (price <= 0.0) {
                unsellable.add(item.clone())
            } else {
                totalMoney += price * item.amount
            }

            val sellEvent = SellEvent(player, item.clone(), totalMoney)
            SellSystem.plugin.server.pluginManager.callEvent(sellEvent)

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
        player.sendRichMessage("<green>Você recebeu <dark_green>$<white>${NumberFormatter.format(totalMoney)}</white> <green>com suas vendas!")
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
