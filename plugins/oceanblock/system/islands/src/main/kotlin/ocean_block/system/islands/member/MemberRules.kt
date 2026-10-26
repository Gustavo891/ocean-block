package ocean_block.system.islands.member

import ocean_block.system.islands.island.Island
import java.util.UUID

enum class InviteCheck(val message: String) {
    Allowed(""),
    NotOwner("<red>Apenas o dono da ilha pode convidar membros."),
    Self("<red>Você já é o dono desta ilha."),
    AlreadyMember("<red>Esse jogador já é membro da ilha."),
    Full("<red>A ilha está lotada de membros. Evolua o upgrade Membros."),
    Blocked("<red>Esse jogador está bloqueado. Desbloqueie em Visitas antes de convidar."),
}

enum class BlockCheck(val message: String) {
    Allowed(""),
    NotOwner("<red>Apenas o dono da ilha pode bloquear jogadores."),
    Self("<red>Você não pode bloquear você mesmo."),
    Member("<red>Você não pode bloquear um membro/confiável de sua ilha. Remova-o primeiro!"),
    AlreadyBlocked("<red>Esse jogador já está bloqueado de acessar sua ilha."),
}

object MemberRules {

    fun checkBlock(island: Island, owner: UUID, target: UUID, targetName: String): BlockCheck = when {
        island.ownerUuid != owner -> BlockCheck.NotOwner
        island.ownerUuid == target -> BlockCheck.Self
        island.isMember(targetName) || island.isTrustedMember(targetName) -> BlockCheck.Member
        island.isBlocked(targetName) -> BlockCheck.AlreadyBlocked
        else -> BlockCheck.Allowed
    }

    fun checkInvite(island: Island, inviter: UUID, target: UUID, targetName: String): InviteCheck = when {
        island.ownerUuid != inviter -> InviteCheck.NotOwner
        else -> checkJoin(island, target, targetName)
    }

    fun checkAccept(island: Island, target: UUID, targetName: String): InviteCheck =
        checkJoin(island, target, targetName)

    private fun checkJoin(island: Island, target: UUID, targetName: String): InviteCheck = when {
        island.ownerUuid == target -> InviteCheck.Self
        island.isMember(targetName) -> InviteCheck.AlreadyMember
        island.blackListNames().contains(targetName.lowercase()) -> InviteCheck.Blocked
        island.members.size >= island.maxMembers() -> InviteCheck.Full
        else -> InviteCheck.Allowed
    }

    fun menuIsland(here: Island?, owned: Island?, player: UUID, playerName: String): Island? =
        here?.takeIf { it.ownerUuid == player || it.isMember(playerName) } ?: owned

    fun homeIsland(owned: Island?, islands: Collection<Island>, playerName: String): Island? =
        owned ?: islands.firstOrNull { it.isMember(playerName) }
}
