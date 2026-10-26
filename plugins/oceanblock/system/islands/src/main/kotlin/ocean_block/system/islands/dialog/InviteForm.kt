package ocean_block.system.islands.dialog

import ocean_block.system.islands.member.BlockCheck
import ocean_block.system.islands.member.InviteCheck

object InviteForm {

    private val NAME = Regex("^[A-Za-z0-9_]{3,16}$")

    fun clean(raw: String?): String = raw.orEmpty().trim()

    fun check(name: String, online: Boolean): String? = when {
        name.isEmpty() -> "DIGITE O NOME DE UM JOGADOR."
        !NAME.matches(name) -> "NOME INVÁLIDO."
        !online -> "${name.uppercase()} NÃO ESTÁ ONLINE."
        else -> null
    }

    fun message(check: InviteCheck): String = when (check) {
        InviteCheck.Allowed -> ""
        InviteCheck.NotOwner -> "SÓ O DONO DA ILHA PODE CONVIDAR."
        InviteCheck.Self -> "VOCÊ JÁ É O DONO DESTA ILHA."
        InviteCheck.AlreadyMember -> "ESSE JOGADOR JÁ É MEMBRO."
        InviteCheck.Full -> "A ILHA ESTÁ CHEIA. EVOLUA O UPGRADE MEMBROS."
        InviteCheck.Blocked -> "ESSE JOGADOR ESTÁ BLOQUEADO NAS VISITAS."
    }

    fun message(check: BlockCheck): String = when (check) {
        BlockCheck.Allowed -> ""
        BlockCheck.NotOwner -> "SÓ O DONO DA ILHA PODE BLOQUEAR."
        BlockCheck.Self -> "VOCÊ NÃO PODE SE BLOQUEAR."
        BlockCheck.Member -> "REMOVA O MEMBRO OU CONFIÁVEL ANTES."
        BlockCheck.AlreadyBlocked -> "ESSE JOGADOR JÁ ESTÁ BLOQUEADO."
    }

    fun sent(name: String): String = "CONVITE ENVIADO PARA ${name.uppercase()}."

    fun blocked(name: String): String = "${name.uppercase()} FOI BLOQUEADO."
}
