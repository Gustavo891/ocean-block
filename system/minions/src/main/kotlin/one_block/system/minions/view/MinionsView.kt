package one_block.system.minions.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.minions.MinionsSystem
import org.bukkit.Material

class MinionsView : View() {

    private val pagination = computedPaginationState(
        { MinionsSystem.recipes }
    ) { context, builder, index, value ->
        builder.renderWith {
            itemStack(value.icon) {
                meta {
                    name("<green>${value.id}")

                    addLore {
                        +""
                        +"<white>Requisitos:"
                        value.requirements.forEach {
                            +"<gray>x${it.amount} ${it.prettyName}"
                        }

                        +""
                        +"<green>Clique para fabricar!"
                    }
                }
            }
        }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Fabricação de minion")
        config.layout(
            "         ",
            "  OOOOO  ",
            "         ",
        )
    }
}
