package one_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta

class IslandsView : View() {

    private val pagination = lazyPaginationState(
        { ctx -> IslandManager.islandsById.values.toMutableList() }
    ) { context, builder, index, value ->
        builder.renderWith { createPhaseItem(value) }
            .onClick { ctx ->
                ctx.player.teleport(value.spawn.toBukkit()!!.add(0.0, 2.0, 0.0))
            }
        //   .onClick { a -> a.openForPlayer(ProductView::class.java, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Ilhas")
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