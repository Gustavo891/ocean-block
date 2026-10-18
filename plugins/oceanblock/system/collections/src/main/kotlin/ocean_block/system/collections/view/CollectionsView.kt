package ocean_block.system.collections.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.collections.collection.CollectionEntry
import ocean_block.system.collections.collection.CollectionManager
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class CollectionsView : View() {

    private val pagination = computedPaginationState(
        { CollectionEntry.entries.toMutableList() }
    ) { context, builder, index, value ->
        builder.withItem(createEntryItem(context, value))
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Coleções")
        config.layout(
            "         ",
            " OOOOOOO ",
            " OOOOOOO ",
            "         ",
        )
    }

    private fun createEntryItem(context: Context, entry: CollectionEntry): ItemStack {
        val data = CollectionManager.get(context.player.uniqueId)

        return itemStack(entry.icon) {
            meta<ItemMeta> {
                name("<green>${entry.prettyName}")

                addLore {
                    if (data == null) {
                        +"<red>Dados não carregados."
                        return@addLore
                    }

                    val amount = data.amountOf(entry)
                    val tier = data.tierOf(entry)
                    val next = data.nextTierAmount(entry)

                    +"<dark_gray>▸ <white>Total: <aqua>${NumberFormatter.format(amount.toDouble())}"
                    +"<dark_gray>▸ <white>Tier: <aqua>$tier<dark_gray>/${entry.maxTier}"
                    +""

                    if (next == null) {
                        +"<gold>Coleção completa."
                    } else {
                        +"<dark_gray>▸ <white>Próximo tier: <yellow>${NumberFormatter.format(next.toDouble())}"
                    }
                }
            }
        }
    }
}
