package ocean_block.system.sell.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.CloseContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.currencies.Currencies
import ocean_block.system.sell.listener.SellEvent
import ocean_block.system.sell.manager.SellManager
import ocean_block.system.sell.notify.SaleNotifier
import ocean_block.system.sell.rules.SaleLine
import ocean_block.system.sell.rules.SaleQuote
import ocean_block.system.sell.rules.SaleRules
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class SellView : View() {

    companion object {
        val SELL_SLOTS = listOf(1..7, 10..16, 19..25).flatMap { it.toList() }
        private val FRAME_MATERIAL = Material.STRUCTURE_VOID
        private val BLANK_MODEL = NamespacedKey("oneblock", "blank")
        private const val TITLE = "<white><shift:-8><glyph:sell_menu><font:oneblock:gui><shift:-170><dark_gray>Venda"
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.title(Mini.mm.deserialize(TITLE))
            .layout(
                "V       V",
                "V       V",
                "V       V",
                "VVVVSVVVV"
            )
            .scheduleUpdate(10)
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('V').renderWith { frameItem() }

        render.layoutSlot('S')
            .renderWith { sellButton(quote(render.inventory)) }
            .onClick { click ->
                click.isCancelled = true
                sell(render.inventory, click.player, returnUnsellable = false)
                render.update()
            }
    }

    override fun onClick(click: SlotClickContext) {
        if (click.clickedContainer.isEntityContainer) return
        if (click.clickedSlot !in SELL_SLOTS) click.isCancelled = true
    }

    override fun onClose(close: CloseContext) {
        sell(close.parent.inventory, close.player, returnUnsellable = true)
    }

    private fun quote(inventory: Inventory): SaleQuote<Material> {
        val lines = SELL_SLOTS.mapNotNull { slot ->
            val item = inventory.getItem(slot) ?: return@mapNotNull null
            if (item.type.isAir || item.type == FRAME_MATERIAL) return@mapNotNull null
            SaleLine(slot, item.type, item.amount)
        }
        return SaleRules.quote(lines, SellManager::getPrice)
    }

    private fun sell(inventory: Inventory, player: Player, returnUnsellable: Boolean) {
        val quote = quote(inventory)

        quote.sold.forEach { line ->
            val item = inventory.getItem(line.slot) ?: return@forEach
            Bukkit.getPluginManager().callEvent(SellEvent(player, item.clone(), line.value))
            inventory.setItem(line.slot, null)
        }

        if (returnUnsellable) {
            quote.unsellable.forEach { line ->
                val item = inventory.getItem(line.slot) ?: return@forEach
                inventory.setItem(line.slot, null)
                giveBack(player, item)
            }
        }

        if (quote.total > 0.0) {
            Currencies.deposit(player.uniqueId, "coins", quote.total)
            SaleNotifier.notify(player, quote.total, quote.sold.sumOf { it.amount })
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f)
            return
        }

        if (!returnUnsellable && quote.unsellable.isNotEmpty()) {
            player.sendRichMessage("<color:#b00f00>Nenhum desses itens tem preço de venda.")
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }
    }

    private fun giveBack(player: Player, item: ItemStack) {
        player.inventory.addItem(item).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }
    }

    private fun frameItem() = itemStack(FRAME_MATERIAL) {
        meta<ItemMeta> {
            isHideTooltip = true
            itemModel = BLANK_MODEL
        }
    }

    private fun sellButton(quote: SaleQuote<Material>) = itemStack(FRAME_MATERIAL) {
        meta<ItemMeta> {
            itemModel = BLANK_MODEL
            if (quote.total > 0.0) {
                name("<green>Vender agora")
                addLore {
                    +"<white>Total: <dark_green>$<white>${NumberFormatter.format(quote.total)}"
                    if (quote.unsellable.isNotEmpty()) +"<gray>Itens sem preço voltam para você."
                    +""
                    +"<yellow>Clique para vender!"
                }
            } else {
                name("<white>Vender")
                addLore {
                    +"<gray>Coloque os itens nos espaços"
                    +"<gray>acima. Ao fechar o menu,"
                    +"<gray>tudo é vendido sozinho."
                }
            }
        }
    }
}
