package ocean_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta

class IslandsView : View() {

    private val pagination = lazyPaginationState(
        { ctx -> IslandManager.islandsById.values.toMutableList() }
    ) { context, builder, index, value ->
        builder.renderWith { createPhaseItem(value) }
            .onClick { ctx ->
                value.teleportTo(ctx.player)
            }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Rafts")
        config.layout(
            "         ",
            "  OOOOO  ",
            "  OOOOO  ",
            "  OOOOO  ",
            "         "
        )
    }

    private fun createPhaseItem(value: Island) = itemStack(Material.GRASS_BLOCK) {
        meta<ItemMeta> {
            name("<yellow>${value.ownerName}")
        }
    }
}
