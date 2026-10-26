package ocean_block.system.islands.dialog

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.member.IslandMembership
import ocean_block.system.islands.view.VisitsView
import one_block.textura.api.DialogBinds
import org.bukkit.entity.Player

object VisitsDialog {

    private const val SCREEN = "ilha_visitas"
    private const val PER_PAGE = 4

    fun open(player: Player, island: Island, viewFrame: ViewFrame) {
        IslandScreens.show(player, SCREEN, { viewFrame.open(VisitsView::class.java, it, island) }) { binds ->
            bind(binds, player, island, viewFrame)
        }
    }

    private fun bind(binds: DialogBinds, player: Player, island: Island, viewFrame: ViewFrame) {
        val owner = IslandScreens.isOwner(island, player)
        var page = 0
        fun pages(): Int = maxOf(1, (island.blackListNames().size + PER_PAGE - 1) / PER_PAGE)
        fun current(): Int = page.coerceIn(0, pages() - 1)

        binds.text("status") { if (island.isPublic) "PÚBLICA" else "PRIVADA" }
        binds.text("status_estilo") { if (island.isPublic) "good" else "bad" }
        binds.text("status_texto") {
            if (island.isPublic) "QUALQUER UM VISITA COM /ILHA VISITAR ${island.ownerName.uppercase()}." else "SÓ MEMBROS E CONFIÁVEIS ENTRAM."
        }
        binds.text("alternar_rotulo") { if (island.isPublic) "FECHAR A ILHA" else "ABRIR A ILHA" }
        binds.text("nao_dono") { (!owner).toString() }
        binds.text("total") { island.blackListNames().size.toString() }
        binds.text("pagina") { "${current() + 1}/${pages()}" }
        binds.text("sem_anterior") { (current() == 0).toString() }
        binds.text("sem_proxima") { (current() >= pages() - 1).toString() }
        binds.list("bloqueados") { IslandRows.blocked(island, owner) }
        binds.list("vazio") { if (island.blackListNames().isEmpty()) listOf(emptyMap()) else emptyList() }
        binds.list("paginador") { if (pages() > 1) listOf(emptyMap()) else emptyList() }
        binds.listPage("bloqueados") { current() }
        binds.listButton("desbloquear") { click, index ->
            IslandRows.blockedName(island, index)?.let { IslandMembership.unblock(click.player, island, it) }
        }
        binds.button("alternar") { click -> IslandMembership.togglePublic(click.player, island) }
        binds.button("bloquear") { click -> NameDialog.block(click, island) { open(it, island, viewFrame) } }
        binds.button("anterior") { page = maxOf(0, current() - 1) }
        binds.button("proxima") { page = minOf(pages() - 1, current() + 1) }
        binds.button("voltar") { click -> IslandScreens.later(click.player) { IslandDialog.open(it, island, viewFrame) } }
    }
}
