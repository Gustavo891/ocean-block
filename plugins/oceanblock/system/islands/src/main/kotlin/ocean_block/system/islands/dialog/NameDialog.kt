package ocean_block.system.islands.dialog

import ocean_block.system.islands.island.Island
import ocean_block.system.islands.member.BlockCheck
import ocean_block.system.islands.member.InviteCheck
import ocean_block.system.islands.member.IslandMembership
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogClick
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player

object NameDialog {

    private const val INPUT = "jogador"

    private class Mode(
        val screen: String,
        val refusal: (Player, Island, Player) -> String?,
        val act: (Player, Island, Player) -> Unit,
        val done: (String) -> String,
        val info: (Island) -> String,
    )

    private val INVITE = Mode(
        "convite",
        { owner, island, target -> IslandMembership.checkInvite(owner, island, target).takeIf { it != InviteCheck.Allowed }?.let(InviteForm::message) },
        { owner, island, target -> IslandMembership.invite(owner, island, target) },
        InviteForm::sent,
        { island -> "${island.members.size}/${island.maxMembers()}" },
    )

    private val BLOCK = Mode(
        "bloquear",
        { owner, island, target -> IslandMembership.checkBlock(owner, island, target).takeIf { it != BlockCheck.Allowed }?.let(InviteForm::message) },
        { owner, island, target -> IslandMembership.block(owner, island, target) },
        InviteForm::blocked,
        { island -> island.blackListNames().size.toString() },
    )

    fun invite(click: DialogClick, island: Island, back: (Player) -> Unit) = open(click, island, INVITE, back)

    fun block(click: DialogClick, island: Island, back: (Player) -> Unit) = open(click, island, BLOCK, back)

    private fun open(click: DialogClick, island: Island, mode: Mode, back: (Player) -> Unit) {
        click.open(mode.screen) { binds -> bind(binds, island, mode, back) }
    }

    private fun bind(binds: DialogBinds, island: Island, mode: Mode, back: (Player) -> Unit) {
        var typed = ""
        var notice = ""
        var done = false
        binds.value(INPUT, { typed }, { typed = it })
        binds.text("aviso") { notice }
        binds.text("aviso_estilo") { if (done) "good" else "bad" }
        binds.text("info") { mode.info(island) }
        binds.button("enviar") { click ->
            val name = InviteForm.clean(click.input(INPUT))
            val target = Bukkit.getPlayerExact(name)
            val error = InviteForm.check(name, target != null) ?: target?.let { mode.refusal(click.player, island, it) }
            if (target == null || error != null) {
                notice = error.orEmpty()
                done = false
                click.player.playSound(click.player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            } else {
                mode.act(click.player, island, target)
                notice = mode.done(target.name)
                done = true
                typed = ""
            }
        }
        binds.button("voltar") { click -> IslandScreens.later(click.player, back) }
    }
}
