package one_block.textura.menu

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.api.ItemApi
import one_block.textura.model.ItemData
import one_block.textura.model.ItemType
import one_block.textura.registry.ItemRegistry
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.function.BooleanSupplier

/**
 * Lista os itens registrados no [ItemRegistry], um por slot, com o
 * `custom_model_data` já aplicado — o ícone é a textura real do pack, não um
 * placeholder. Clique dá uma cópia do item ao jogador.
 *
 * Aberto por `/textura itens`.
 */
class ItemsMenu(private val itemRegistry: ItemRegistry) : View() {

    private val pagination = lazyPaginationState<ItemData>(
        { _ -> itemRegistry.items.values.sortedBy { it.id }.toMutableList() }
    ) { _, builder, _, value ->
        builder.renderWith { createItem(value) }
            .onClick { click -> giveItem(click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Itens do Textura")
        config.layout(
            "         ",
            "  OOOOO  ",
            "< OOOOO >",
            "  OOOOO  ",
            "         ",
            "         "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = pagination.get(render)

        render.layoutSlot('<')
            .updateOnStateChange(this.pagination)
            .renderWith { ItemStack(Material.ARROW) }
            .displayIf(BooleanSupplier { pagination.canBack() })
            .onClick(pagination::back)

        render.layoutSlot('>')
            .updateOnStateChange(this.pagination)
            .renderWith { ItemStack(Material.ARROW) }
            .displayIf(BooleanSupplier { pagination.canAdvance() })
            .onClick(pagination::advance)

        render.slot(3, 5)
            .renderWith { createEmptyItem() }
            .displayIf(BooleanSupplier { itemRegistry.items.isEmpty() })
    }

    /**
     * Monta o ItemStack de um item registrado. Material inválido no yml não
     * derruba o menu: cai em BARRIER e o lore avisa qual era o material.
     */
    private fun createItem(data: ItemData): ItemStack {
        val material = Material.matchMaterial(data.material)
        val item = ItemApi.create(data.id) ?: ItemStack(Material.BARRIER)

        item.editMeta { meta ->
            meta.displayName(
                Component.text(data.id, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )

            val lore = mutableListOf<Component>()
            lore += line("Material", if (material == null) "${data.material} (inválido)" else data.material)
            lore += line("Tipo", data.type.name)
            if (data.hasCustomModelData) lore += line("custom_model_data", data.customModelData.toString())
            data.itemModel?.let { lore += line("item_model", it) }
            when (data.type) {
                ItemType.FLAT -> data.texturePath?.let { lore += line("Textura", it) }
                ItemType.MODEL -> data.modelPath?.let { lore += line("Modelo", it) }
            }
            lore += Component.empty()
            lore += Component.text("Clique para receber o item.", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
            meta.lore(lore)
        }

        return item
    }

    private fun createEmptyItem(): ItemStack {
        val item = ItemStack(Material.BARRIER)
        item.editMeta { meta ->
            meta.displayName(
                Component.text("Nenhum item registrado", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(line("Pasta", "plugins/Textura/items")))
        }
        return item
    }

    private fun giveItem(click: SlotClickContext, data: ItemData) {
        val item = createItem(data)
        item.editMeta { meta -> meta.lore(null) }

        val leftover = click.player.inventory.addItem(item)
        if (leftover.isNotEmpty()) {
            click.player.sendMessage(
                Component.text("Inventário cheio.", NamedTextColor.RED)
            )
            return
        }

        click.player.sendMessage(
            Component.text("Você recebeu '${data.id}'.", NamedTextColor.GREEN)
        )
    }

    private fun line(label: String, value: String): Component =
        Component.text("$label: ", NamedTextColor.GRAY)
            .append(Component.text(value, NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false)
}
