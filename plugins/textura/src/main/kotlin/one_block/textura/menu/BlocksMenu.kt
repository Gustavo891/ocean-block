package one_block.textura.menu

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.api.BlockApi
import one_block.textura.block.NoteBlockAllocator
import one_block.textura.model.BlockDefinition
import one_block.textura.model.BlockEntryType
import one_block.textura.registry.BlockRegistry
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.function.BooleanSupplier

class BlocksMenu(
    private val blockRegistry: BlockRegistry,
    private val allocator: NoteBlockAllocator,
) : View() {

    private val pagination = lazyPaginationState<BlockDefinition>(
        { _ -> blockRegistry.blocks.values.sortedWith(compareBy({ it.type }, { it.id })).toMutableList() }
    ) { _, builder, _, value ->
        builder.renderWith { createItem(value) }
            .onClick { click -> giveItem(click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Blocos do Textura")
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
            .displayIf(BooleanSupplier { blockRegistry.blocks.isEmpty() })
    }

    private fun createItem(data: BlockDefinition): ItemStack {
        val item = BlockApi.createItem(data.id) ?: ItemStack(Material.BARRIER)

        item.editMeta { meta ->
            meta.displayName(
                Component.text(data.name ?: data.id, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )

            val lore = mutableListOf<Component>()
            lore += line("Id", data.id)
            lore += line("Tipo", data.type.name)

            when (data.type) {
                BlockEntryType.OVERRIDE -> {
                    lore += line("Alvo", data.target ?: "?")
                    data.texture?.let { lore += line("Textura", it) }
                }
                BlockEntryType.CUSTOM -> {
                    lore += line("Base", data.base.name)
                    lore += line("Blockstate", allocator.propertiesOf(data.id) ?: "sem alocacao")
                    lore += line("Item", data.itemMaterial)
                    data.model?.let { lore += line("Modelo", it) }
                    data.texture?.let { lore += line("Textura", it) }
                    data.textures.forEach { (face, caminho) -> lore += line("Textura[$face]", caminho) }
                }
            }

            if (data.type == BlockEntryType.CUSTOM) {
                lore += line("Drop do proprio item", if (data.defaultDrop) "sim" else "nao")
            }
            lore += line("Drop", data.dropId ?: "nenhum")

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
                Component.text("Nenhum bloco registrado", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false)
            )
            meta.lore(listOf(line("Pasta", "plugins/Textura/configs/blocks")))
        }
        return item
    }

    private fun giveItem(click: SlotClickContext, data: BlockDefinition) {
        val item = BlockApi.createItem(data.id)
        if (item == null) {
            click.player.sendMessage(
                Component.text("Bloco '${data.id}' esta mal configurado.", NamedTextColor.RED)
            )
            return
        }

        val leftover = click.player.inventory.addItem(item)
        if (leftover.isNotEmpty()) {
            click.player.sendMessage(Component.text("Inventário cheio.", NamedTextColor.RED))
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
