package one_block.system.sell.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.libs.formatter.Mini
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.sell.manager.SellManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.BooleanSupplier

class SellItemsView : View() {

    private val pagination = lazyPaginationState(
        { _: Context -> SellManager.all().toMutableList() }
    ) { _, builder, _, value: Pair<Material, Double> ->
        builder.renderWith { createItem(value.first, value.second) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(
            Mini.mm.deserialize("<white><shift:-8><glyph:sell_items><font:oneblock:gui><shift:-170><dark_gray>Valores dos Itens")
        )
        config.layout(
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "1<<< >>>2",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = pagination.get(render)

        render.layoutSlot('1')
            .updateOnStateChange(this.pagination)
            .renderWith { navIcon("button_left") }
            .displayIf(BooleanSupplier { pagination.canBack() })

        render.layoutSlot('2')
            .updateOnStateChange(this.pagination)
            .renderWith { navIcon("button_right") }
            .displayIf(BooleanSupplier { pagination.canAdvance() })

        render.layoutSlot('<')
            .updateOnStateChange(this.pagination)
            .renderWith { navIcon("blank") }
            .displayIf(BooleanSupplier { pagination.canBack() })
            .onClick { click ->
                pagination.back()
                render.update()
                click.player.playSound(click.player, "oneblock:button.click", 1.0f, 1.0f)
            }

        render.layoutSlot('>')
            .updateOnStateChange(this.pagination)
            .renderWith { navIcon("blank") }
            .displayIf(BooleanSupplier { pagination.canAdvance() })
            .onClick { click ->
                pagination.advance()
                render.update()
                click.player.playSound(click.player, "oneblock:button.click", 1.0f, 1.0f)
            }

    }

    private fun navIcon(model: String): ItemStack = itemStack(Material.PAPER) {
        meta<ItemMeta> {
            itemModel = NamespacedKey("oneblock", model)
            isHideTooltip = true
        }
    }

    private fun createItem(material: Material, price: Double): ItemStack = itemStack(material) {
        meta<ItemMeta> {
            name(
                Component.translatable(material.translationKey())
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )
            addLore {
                +"<white>Vende por: <green>$<white>${NumberFormatter.format(price)}"
            }
        }
    }
}
