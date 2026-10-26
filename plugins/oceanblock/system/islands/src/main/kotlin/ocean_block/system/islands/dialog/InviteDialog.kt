package ocean_block.system.islands.dialog

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.instance.Instance
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.member.InviteCheck
import ocean_block.system.islands.member.IslandMembership
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogClick
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player

object InviteDialog {

    private const val SCREEN = "convite"
    private const val INPUT = "jogador"

    fun open(click: DialogClick, island: Island, viewFrame: ViewFrame) {
        click.open(SCREEN) { binds -> bind(binds, island, viewFrame) }
    }

    private fun bind(binds: DialogBinds, island: Island, viewFrame: ViewFrame) {
        var typed = ""
        var notice = ""
        var sent = false
        binds.value(INPUT, { typed }, { typed = it })
        binds.text("aviso") { notice }
        binds.text("aviso_estilo") { if (sent) "good" else "bad" }
        binds.text("vagas") { "${island.members.size}/${island.maxMembers()}" }
        binds.button("convidar") { click ->
            val name = InviteForm.clean(click.input(INPUT))
            val target = Bukkit.getPlayerExact(name)
            val error = InviteForm.check(name, target != null) ?: target?.let { refusal(click.player, island, it) }
            if (target == null || error != null) {
                notice = error.orEmpty()
                sent = false
                click.player.playSound(click.player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            } else {
                IslandMembership.invite(click.player, island, target)
                notice = InviteForm.sent(target.name)
                sent = true
                typed = ""
            }
        }
        binds.button("voltar") { click ->
            click.close()
            val player = click.player
            Bukkit.getScheduler().runTask(Instance, Runnable { if (player.isOnline) IslandDialog.open(player, island, viewFrame) })
        }
    }

    private fun refusal(owner: Player, island: Island, target: Player): String? =
        IslandMembership.checkInvite(owner, island, target).takeIf { it != InviteCheck.Allowed }?.let(InviteForm::message)
}
