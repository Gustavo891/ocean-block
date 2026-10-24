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
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class VisitsView : View() {

    private val islandState = initialState<Island>()

    private val pagination = computedPaginationState(
        { ctx -> islandState.get(ctx).blackListNames().sorted().toMutableList() }
    ) { ctx, builder, _, name ->
        builder.renderWith { blockedItem(name, isOwner(ctx)) }
            .onClick { click -> unblock(click, name) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(menuTitle("ilha_visitas", "Visitas"))
        config.layout(
            "  p   b  ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < v >  ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val island = islandState.get(render)

        render.layoutSlot('p')
            .renderWith { publicItem(island, isOwner(render)) }
            .onClick { click ->
                if (IslandMembership.togglePublic(click.player, island)) click.update()
            }
        render.layoutSlot('b')
            .renderWith { blockItem(isOwner(render)) }
            .onClick { click ->
                if (!isOwner(click)) {
                    click.player.sendRichMessage("<red>Apenas o dono da ilha pode bloquear jogadores.")
                    return@onClick
                }
                click.openForPlayer(PlayerPickerView::class.java, PlayerPick(island, PlayerPick.Mode.Block))
            }
        render.layoutSlot('<')
            .renderWith { blankItem("<yellow>Página anterior") }
            .onClick { click -> pagination.get(click).back() }
        render.layoutSlot('>')
            .renderWith { blankItem("<yellow>Próxima página") }
            .onClick { click -> pagination.get(click).advance() }
        render.layoutSlot('v')
            .renderWith { backItem() }
            .onClick { click -> click.openForPlayer(IslandView::class.java, island) }
    }

    private fun unblock(click: SlotClickContext, name: String) {
        if (!isOwner(click)) return
        if (IslandMembership.unblock(click.player, islandState.get(click), name)) {
            pagination.get(click).forceUpdate()
            click.update()
        }
    }

    private fun isOwner(context: Context) = islandState.get(context).ownerUuid == context.player.uniqueId

    private fun publicItem(island: Island, viewerIsOwner: Boolean): ItemStack =
        itemStack(if (island.isPublic) Material.LIME_DYE else Material.GRAY_DYE) {
            meta {
                name(if (island.isPublic) "<green>Ilha pública" else "<red>Ilha privada")
                setLore {
                    +(if (island.isPublic) "<gray>Qualquer um pode visitar com" else "<gray>Só membros e confiáveis entram.")
                    if (island.isPublic) +"<white>/ilha visitar ${island.ownerName}<gray>."
                    +""
                    +(if (viewerIsOwner) "<yellow>Clique para ${if (island.isPublic) "fechar" else "abrir"} a ilha!" else "<dark_gray>Apenas o dono pode alterar.")
                }
            }
        }

    private fun blockItem(viewerIsOwner: Boolean): ItemStack = itemStack(Material.BARRIER) {
        meta {
            name("<red>Bloquear jogador")
            setLore {
                +"<gray>Um jogador bloqueado não entra"
                +"<gray>nem visita a sua ilha."
                +""
                +(if (viewerIsOwner) "<yellow>Clique para escolher um jogador online!" else "<dark_gray>Apenas o dono pode bloquear.")
            }
            addItemFlags(*ItemFlag.entries.toTypedArray())
        }
    }

    private fun blockedItem(name: String, viewerIsOwner: Boolean): ItemStack = headOf(name) {
        name("<red>$name")
        setLore {
            +"<gray>Bloqueado"
            if (viewerIsOwner) {
                +""
                +"<yellow>Clique para desbloquear."
            }
        }
    }
}
