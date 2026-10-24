package ocean_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.member.IslandMembership
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class PlayerPick(val island: Island, val mode: Mode) {
    enum class Mode(val title: String, val action: String) {
        Invite("Convidar membro", "<yellow>Clique para convidar!"),
        Block("Bloquear jogador", "<red>Clique para bloquear!"),
    }
}

class PlayerPickerView : View() {

    private val pickState = initialState<PlayerPick>()

    private val pagination = computedPaginationState(
        { ctx -> candidates(ctx.player, pickState.get(ctx)).toMutableList() }
    ) { ctx, builder, _, target ->
        builder.renderWith { candidateItem(target, pickState.get(ctx).mode) }
            .onClick { click -> pick(click, target) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < v >  ",
        )
    }

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title(menuTitle("ilha_jogadores", pickState.get(open).mode.title))
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('<')
            .renderWith { blankItem("<yellow>Página anterior") }
            .onClick { click -> pagination.get(click).back() }
        render.layoutSlot('>')
            .renderWith { blankItem("<yellow>Próxima página") }
            .onClick { click -> pagination.get(click).advance() }
        render.layoutSlot('v')
            .renderWith { backItem() }
            .onClick { click -> back(click) }
    }

    private fun pick(click: SlotClickContext, target: Player) {
        val pick = pickState.get(click)
        if (!target.isOnline) {
            click.player.sendRichMessage("<red>Esse jogador saiu do servidor.")
            refresh(click)
            return
        }
        val done = when (pick.mode) {
            PlayerPick.Mode.Invite -> IslandMembership.invite(click.player, pick.island, target)
            PlayerPick.Mode.Block -> IslandMembership.block(click.player, pick.island, target)
        }
        if (done) back(click) else refresh(click)
    }

    private fun back(click: SlotClickContext) {
        val pick = pickState.get(click)
        val view = when (pick.mode) {
            PlayerPick.Mode.Invite -> MembersView::class.java
            PlayerPick.Mode.Block -> VisitsView::class.java
        }
        click.openForPlayer(view, pick.island)
    }

    private fun refresh(click: SlotClickContext) {
        pagination.get(click).forceUpdate()
        click.update()
    }

    private fun candidates(viewer: Player, pick: PlayerPick): List<Player> {
        val island = pick.island
        return Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != viewer.uniqueId && it.uniqueId != island.ownerUuid && !island.isMember(it.name) }
            .filter {
                when (pick.mode) {
                    PlayerPick.Mode.Invite -> !island.isBlocked(it.name)
                    PlayerPick.Mode.Block -> !island.isBlocked(it.name) && !island.isTrustedMember(it.name)
                }
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun candidateItem(target: Player, mode: PlayerPick.Mode): ItemStack = headOf(target) {
        name("<white>${target.name}")
        setLore { +mode.action }
    }
}
