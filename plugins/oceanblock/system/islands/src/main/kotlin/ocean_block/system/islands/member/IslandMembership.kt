package ocean_block.system.islands.member

import ocean_block.libs.formatter.Mini
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandMember
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

object IslandMembership {

    private const val INVITE_TTL_MILLIS = 60_000L

    private val invites = IslandInvites(INVITE_TTL_MILLIS)

    fun menuIsland(player: Player): Island? = MemberRules.menuIsland(
        IslandManager.getIslandAt(player.location),
        IslandManager.getIslandByOwnerUUID(player.uniqueId),
        player.uniqueId,
        player.name,
    )

    fun checkInvite(owner: Player, island: Island, target: Player): InviteCheck =
        MemberRules.checkInvite(island, owner.uniqueId, target.uniqueId, target.name)

    fun invite(owner: Player, island: Island, target: Player): Boolean {
        val check = checkInvite(owner, island, target)
        if (check != InviteCheck.Allowed) return deny(owner, check.message)

        invites.add(target.uniqueId, island.uuid, island.ownerName, System.currentTimeMillis())
        owner.sendRichMessage("<green>Convite enviado para <white>${target.name}</white>. Ele tem 60 segundos para aceitar.")
        owner.playSound(owner, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f)
        target.sendMessage(
            Mini.mm.deserialize(
                "<yellow>${owner.name} <gray>convidou você para ser membro da ilha de <white>${island.ownerName}</white>. " +
                    "<click:run_command:'/ilha aceitar ${island.ownerName}'><hover:show_text:'<green>Clique para entrar na ilha'>" +
                    "<green><bold>[ACEITAR]</bold></hover></click>"
            )
        )
        target.playSound(target, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.4f)
        return true
    }

    fun accept(player: Player, ownerName: String?) {
        val now = System.currentTimeMillis()
        val pending = invites.pending(player.uniqueId, now)
        if (ownerName == null && pending.size > 1) {
            deny(player, "<red>Você tem mais de um convite. Use <white>/ilha aceitar <dono></white>.")
            return
        }
        val invite = invites.take(player.uniqueId, ownerName, now)
        if (invite == null) {
            deny(player, "<red>Você não tem convite válido. Convites expiram em 60 segundos.")
            return
        }
        val island = IslandManager.islandsById[invite.islandUuid]
        if (island == null) {
            deny(player, "<red>Essa ilha não existe mais.")
            return
        }
        val check = MemberRules.checkAccept(island, player.uniqueId, player.name)
        if (check != InviteCheck.Allowed) {
            deny(player, check.message)
            return
        }

        island.addMember(player.name, IslandMember(player.name, mutableSetOf()))
        island.markDirty()
        team(island).filter { it != player }.forEach {
            it.sendRichMessage("<green>${player.name} agora é membro da ilha!")
        }
        player.sendRichMessage("<green>Agora você é membro da ilha de <white>${island.ownerName}</white>. Use <white>/ilha ${island.ownerName}</white> para ir até ela.")
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f)
    }

    fun remove(owner: Player, island: Island, memberName: String): Boolean {
        if (island.ownerUuid != owner.uniqueId) return deny(owner, "<red>Apenas o dono da ilha pode remover membros.")
        if (island.members.remove(memberName.lowercase()) == null) return deny(owner, "<red>Esse jogador não faz parte da sua ilha.")

        island.markDirty()
        owner.sendRichMessage("<green>O jogador <white>$memberName</white> foi removido da sua ilha.")
        owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
        Bukkit.getPlayerExact(memberName)?.sendRichMessage("<red>Você foi removido da ilha de ${island.ownerName}.")
        return true
    }

    fun block(owner: Player, island: Island, target: Player): Boolean {
        if (island.ownerUuid != owner.uniqueId) return deny(owner, "<red>Apenas o dono da ilha pode bloquear jogadores.")
        if (island.ownerUuid == target.uniqueId) return deny(owner, "<red>Você não pode bloquear você mesmo.")
        if (island.isMember(target.name) || island.isTrustedMember(target.name)) {
            return deny(owner, "<red>Você não pode bloquear um membro/confiável de sua ilha. Remova-o primeiro!")
        }
        if (island.isBlocked(target.name)) return deny(owner, "<red>Esse jogador já está bloqueado de acessar sua ilha.")

        island.addBlacklist(target)
        island.markDirty()
        owner.sendRichMessage("<green>O jogador <white>${target.name}</white> foi bloqueado de acessar sua ilha.")
        return true
    }

    fun unblock(owner: Player, island: Island, targetName: String): Boolean {
        if (island.ownerUuid != owner.uniqueId) return deny(owner, "<red>Apenas o dono da ilha pode desbloquear jogadores.")
        if (!island.removeBlacklist(targetName)) return deny(owner, "<red>Esse jogador não está bloqueado de acessar sua ilha.")

        island.markDirty()
        owner.sendRichMessage("<green>O jogador <white>$targetName</white> foi liberado de acessar sua ilha.")
        return true
    }

    fun togglePublic(owner: Player, island: Island): Boolean {
        if (island.ownerUuid != owner.uniqueId) return deny(owner, "<red>Apenas o dono da ilha pode mudar as visitas.")

        island.isPublic = !island.isPublic
        island.markDirty()
        owner.playSound(owner, if (island.isPublic) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
        return true
    }

    fun forget(player: UUID) {
        invites.forget(player)
    }

    private fun team(island: Island): List<Player> = Bukkit.getOnlinePlayers()
        .filter { it.uniqueId == island.ownerUuid || island.isMember(it.name) }

    private fun deny(player: Player, message: String): Boolean {
        player.sendRichMessage(message)
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
        return false
    }
}
