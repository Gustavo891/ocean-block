package ocean_block.system.islands.dialog

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.instance.Instance
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.member.IslandMembership
import ocean_block.system.islands.view.MembersView
import one_block.textura.api.DialogBinds
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object MembersDialog {

    private const val SCREEN = "ilha_membros"

    fun open(player: Player, island: Island, viewFrame: ViewFrame) {
        IslandPortraits.cards(island).whenComplete { cards, error ->
            if (error != null) Instance.logger.warning("[ilha] membros sem rostos: ${error.message}")
            IslandScreens.later(player) { show(it, island, viewFrame, cards) }
        }
    }

    private fun show(player: Player, island: Island, viewFrame: ViewFrame, cards: IslandPortraits.Cards?) {
        IslandScreens.show(player, SCREEN, { viewFrame.open(MembersView::class.java, it, island) }) { binds ->
            bind(binds, player, island, viewFrame, cards)
        }
    }

    private fun bind(binds: DialogBinds, player: Player, island: Island, viewFrame: ViewFrame, cards: IslandPortraits.Cards?) {
        val owner = IslandScreens.isOwner(island, player)
        cards?.let { binds.pixels("cartoes") { it.image } }
        (0 until MemberCards.SLOTS).forEach { index ->
            val name = cards?.members?.getOrNull(index)
            binds.text("cartao$index") { IslandPortraits.tooltip(name, index) }
            binds.button("clicar_cartao$index") { click ->
                if (name == null && owner) NameDialog.invite(click, island) { open(it, island, viewFrame) }
            }
        }
        binds.text("vagas") { "${island.members.size}/${island.maxMembers()}" }
        binds.text("nao_dono") { (!owner).toString() }
        binds.list("membros") { IslandRows.members(island, owner) { Bukkit.getPlayerExact(it) != null } }
        binds.listButton("remover") { click, index ->
            val name = IslandRows.memberName(island, index) ?: return@listButton
            if (IslandMembership.remove(click.player, island, name)) open(click.player, island, viewFrame)
        }
        binds.button("convidar") { click -> NameDialog.invite(click, island) { open(it, island, viewFrame) } }
        binds.button("voltar") { click -> IslandScreens.later(click.player) { IslandDialog.open(it, island, viewFrame) } }
    }
}
