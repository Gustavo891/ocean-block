package ocean_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.member.IslandMembership
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class MembersView : View() {

    private data class Entry(val name: String, val owner: Boolean)

    private val islandState = initialState<Island>()

    private val pagination = computedPaginationState(
        { ctx -> entries(islandState.get(ctx)).toMutableList() }
    ) { ctx, builder, _, entry ->
        builder.renderWith { memberItem(islandState.get(ctx), entry, isOwner(ctx)) }
            .onClick { click -> remove(click, entry) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(menuTitle("ilha_membros", "Membros"))
        config.layout(
            " OOOOOOO ",
            "         ",
            "   v a   ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val island = islandState.get(render)

        render.layoutSlot('v')
            .renderWith { backItem() }
            .onClick { click -> click.openForPlayer(IslandView::class.java, island) }
        render.layoutSlot('a')
            .renderWith { inviteItem(island, isOwner(render)) }
            .onClick { click ->
                if (!isOwner(click)) {
                    click.player.sendRichMessage("<red>Apenas o dono da ilha pode convidar membros.")
                    return@onClick
                }
                click.openForPlayer(PlayerPickerView::class.java, PlayerPick(island, PlayerPick.Mode.Invite))
            }
    }

    private fun remove(click: SlotClickContext, entry: Entry) {
        if (entry.owner || !isOwner(click)) return
        if (IslandMembership.remove(click.player, islandState.get(click), entry.name)) {
            pagination.get(click).forceUpdate()
            click.update()
        }
    }

    private fun entries(island: Island): List<Entry> =
        listOf(Entry(island.ownerName, owner = true)) +
            island.members.values.map { Entry(it.name, owner = false) }.sortedBy { it.name.lowercase() }

    private fun isOwner(context: Context) = islandState.get(context).ownerUuid == context.player.uniqueId

    private fun memberItem(island: Island, entry: Entry, viewerIsOwner: Boolean): ItemStack = headOf(entry.name) {
        name(if (entry.owner) "<gold>${entry.name}" else "<white>${entry.name}")
        setLore {
            +(if (entry.owner) "<gray>Dono da ilha" else "<gray>Membro")
            if (!entry.owner && viewerIsOwner) {
                +""
                +"<red>Clique para remover da ilha."
            }
        }
    }

    private fun inviteItem(island: Island, viewerIsOwner: Boolean): ItemStack = itemStack(Material.LIME_DYE) {
        meta {
            name("<green>Convidar membro")
            setLore {
                +"<gray>Vagas: <white>${island.members.size}/${island.maxMembers()}"
                +""
                +(if (viewerIsOwner) "<yellow>Clique para escolher um jogador online!" else "<dark_gray>Apenas o dono pode convidar.")
            }
        }
    }
}
