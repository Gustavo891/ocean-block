package one_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.*
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandMember
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta

class IslandMembersView() : View() {

    private val pagination = lazyPaginationState(
        { ctx ->
            val island = IslandManager.getIslandByPlayer(ctx.player.uniqueId)
            ctx.player.sendMessage("Island: ${island?.members}")
            island?.members?.values?.toMutableList()
        }
    ) { context, builder, index, value ->
        builder.renderWith { createMemberItem(value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "         ",
            "  OOOOO  ",
            "  OOOOO  ",
            "  OOOOO  ",
            "    v    "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('v')
            .renderWith { itemStack(Material.ARROW) {
                meta<ItemMeta> {
                    name("<yellow>Voltar")
                }
            } }
            .onClick { _ ->
                render.openForPlayer(IslandView::class.java)
            }
    }

    fun createMemberItem(member: IslandMember) = itemStack(Material.PLAYER_HEAD) {
        meta<ItemMeta> {
            name("<yellow>${member.name}")
            addLore {
                + "<white>Permissão: <gray> -/-"
            }
        }

    }

}