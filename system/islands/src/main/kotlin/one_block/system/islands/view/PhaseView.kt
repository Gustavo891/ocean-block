package one_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.translation.translate
import one_block.system.islands.phase.Phase
import one_block.system.islands.phase.PhaseManager
import org.bukkit.inventory.meta.ItemMeta

class PhaseView : View() {

    private val pagination = lazyPaginationState(
        { ctx -> PhaseManager.phases() }
    ) { context, builder, index, value ->
        builder.renderWith { createPhaseItem(value) }
        //   .onClick { a -> a.openForPlayer(ProductView::class.java, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Fases")
        config.layout(
            "         ",
            "  OOOOO  ",
            "  OOOOO  ",
            "  OOOOO  ",
            "         "
        )
    }

    private fun createPhaseItem(value: Phase) = itemStack(value.iconMaterial) {
        try {
            meta<ItemMeta> {
                name("<yellow>${value.prettyName}")

                addLore {
                    +"<dark_gray>Blocos necessarios: ${value.blocksToNextPhase}"
                    +""
                    +" <white>Blocos"
                    value.blockPool.forEach { material, i ->
                        +" <dark_gray>▪ <gray>${translate(material.translationKey())}"
                    }

                    +""

                    +" <white>Mobs"
                    value.mobPool.forEach { type, data ->
                        +" <dark_gray>▪ <gray>${translate(type.translationKey())}"
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}