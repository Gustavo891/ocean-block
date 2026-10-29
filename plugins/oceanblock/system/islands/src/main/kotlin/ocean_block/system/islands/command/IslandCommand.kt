package ocean_block.system.islands.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.islands.choice.IslandChoice
import ocean_block.system.islands.dialog.IslandDialog
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.member.IslandMembership
import ocean_block.system.islands.phase.PhaseManager
import ocean_block.system.islands.reset.IslandResetService
import ocean_block.system.islands.view.IslandView
import ocean_block.system.islands.view.IslandsView
import org.bukkit.entity.Player

@CommandAlias("ilha|island|is")
class IslandCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    private fun ownedIslandOf(sender: Player): Island? {
        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Você não tem ilha!")
            return null
        }
        if (island.ownerUuid != sender.uniqueId) {
            sender.sendRichMessage("<red>Você não é o dono desta ilha!")
            return null
        }
        return island
    }

    @Default
    fun onDefault(sender: Player, @Optional targetName: String?) {
        if (targetName != null) {
            goToOther(sender, targetName)
            return
        }

        if (IslandManager.getHomeIsland(sender) == null) {
            IslandChoice.begin(sender)
            return
        }
        val island = IslandMembership.menuIsland(sender) ?: return openMenu(sender)
        IslandDialog.open(sender, island, viewFrame)
    }

    private fun goToOther(sender: Player, targetName: String) {
        val playerIsland = IslandManager.getIslandByOwnerName(targetName.lowercase())
        if (playerIsland == null) {
            sender.sendRichMessage("<red>Esse jogador não tem uma ilha!")
            return
        }
        if (playerIsland.ownerUuid == sender.uniqueId || playerIsland.isMember(sender.name) || playerIsland.isTrustedMember(sender.name)) {
            playerIsland.teleportTo(sender)
            return
        }
        sender.sendRichMessage("<red>Você não é membro dessa ilha. Se ela for pública, use <white>/ilha visitar $targetName</white>.")
    }

    private fun openMenu(sender: Player) {
        val island = IslandMembership.menuIsland(sender)
        if (island == null) {
            sender.sendRichMessage("<red>Você não tem ilha! Use <white>/ilha</white> para criar a sua.")
            return
        }
        viewFrame.open(IslandView::class.java, sender, island)
    }

    @Subcommand("ir|home")
    fun onGo(sender: Player) {
        val island = IslandManager.getHomeIsland(sender)
        if (island == null) {
            IslandChoice.begin(sender)
            return
        }
        island.teleportTo(sender)
    }

    @Subcommand("menu")
    fun onMenu(sender: Player) {
        openMenu(sender)
    }

    @Subcommand("aceitar")
    fun onAccept(sender: Player, @Optional ownerName: String?) {
        IslandMembership.accept(sender, ownerName)
    }

    @Subcommand("remove|remover")
    fun onRemove(sender: Player, targetName: String) {
        val island = ownedIslandOf(sender) ?: return
        IslandMembership.remove(sender, island, targetName)
    }

    @Subcommand("undeny|desbloquear")
    fun unBlacklist(sender: Player, target: String) {
        val island = ownedIslandOf(sender) ?: return
        IslandMembership.unblock(sender, island, target)
    }

    @Subcommand("deny|bloquear")
    fun blackList(sender: Player, target: OnlinePlayer) {
        val island = ownedIslandOf(sender) ?: return
        IslandMembership.block(sender, island, target.player)
    }

    @Subcommand("visitar|v")
    fun onVisit(sender: Player, targetName: String) {
        val island = IslandManager.getIslandByOwnerName(targetName)
        if (island == null) {
            sender.sendRichMessage("<red>Este jogador não possui uma ilha!")
            return
        }

        if (island.isBlocked(sender.name)) {
            sender.sendRichMessage("<red>Você está proibido de acessar esta ilha.")
            return
        }

        if (!island.isPublic) {
            sender.sendRichMessage("<red>A ilha deste jogador não é pública.")
            return
        }

        val spawn = island.spawn.toBukkit()
        if (spawn == null) {
            sender.sendRichMessage("<red>Erro: mundo não carregado.")
            return
        }

        sender.teleport(spawn.add(0.0, 2.0, 0.0))
        island.onJoin(sender)
        IslandManager.playerIsland[sender.uniqueId] = island.uuid
        sender.fallDistance = 0f

        sender.sendRichMessage("<green>Teleportado para a ilha de ${targetName}.")
    }

    @Subcommand("publica|pub")
    fun onPublic(sender: Player, isPublic: Boolean) {
        val island = ownedIslandOf(sender) ?: return
        island.isPublic = isPublic
        island.markDirty()

        sender.sendRichMessage(if (isPublic) "<green>Sua ilha agora é pública." else "<green>Sua ilha agora é privada.")
    }

    @Subcommand("sethome")
    fun onSetHome(sender: Player) {
        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Você precisa estar em uma ilha.")
            return
        }

        if (island.ownerUuid != sender.uniqueId) {
            sender.sendRichMessage("<red>Você não é o dono dessa ilha.")
            return
        }

        if (IslandManager.getIslandAt(sender.location)?.uuid != island.uuid) {
            sender.sendRichMessage("<red>Você só pode definir a home dentro da sua ilha.")
            return
        }

        island.spawn = SimpleLocation.fromBukkit(sender.location)
        island.markDirty()
        sender.sendRichMessage("<green>Seu home foi definido.")
    }

    @Subcommand("reset terrain")
    @CommandPermission("screen.dev")
    fun onResetTerrain(sender: Player, targetName: String, @Optional flag: String?) {
        val island = IslandManager.getIslandByOwnerName(targetName.lowercase())
        if (island == null) {
            sender.sendRichMessage("<red>Esse jogador não tem uma ilha!")
            return
        }

        if (!flag.equals("-confirm", true)) {
            IslandResetService.previewTerrain(sender, island)
            return
        }

        IslandResetService.resetTerrain(sender, island)
    }

    @Subcommand("resetplayer")
    @CommandPermission("screen.dev")
    fun onResetPlayer(sender: Player, targetName: String, @Optional flag: String?) {
        val island = IslandManager.getIslandByOwnerName(targetName.lowercase())
        if (island == null) {
            sender.sendRichMessage("<red>Esse jogador não é dono de uma ilha. Nada foi alterado.")
            return
        }

        if (!flag.equals("-confirm", true)) {
            IslandResetService.previewDelete(sender, island)
            return
        }

        IslandResetService.deleteIsland(sender, island)
    }

    @Subcommand("reset progress")
    @CommandPermission("screen.dev")
    fun onResetProgress(sender: Player, targetName: String, @Optional flag: String?) {
        val island = IslandManager.getIslandByOwnerName(targetName.lowercase())
        if (island == null) {
            sender.sendRichMessage("<red>Esse jogador não tem uma ilha!")
            return
        }

        if (!flag.equals("-confirm", true)) {
            IslandResetService.previewProgress(sender, island)
            return
        }

        IslandResetService.resetProgress(sender, island)
    }

    @Subcommand("reload")
    @CommandPermission("screen.dev")
    fun onReload(sender: Player) {
        PhaseManager.loadPhases()
        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }

    @Subcommand("all")
    @CommandPermission("screen.dev")
    fun onAll(sender: Player) {
        viewFrame.open(IslandsView::class.java, sender)
    }

    @Subcommand("trust")
    fun onTrust(sender: Player, targetOnline: OnlinePlayer) {
        val target = targetOnline.player

        val island = ownedIslandOf(sender) ?: return

        if (sender.name.equals(target.name, true)) {
            sender.sendRichMessage("<red>Você quer se adicionar? Não entendi.")
            return
        }

        if (!island.addTrustedMember(target.name)) {
            sender.sendRichMessage("<green>Este jogador já está adicionado")
            return
        }

        island.markDirty()
        sender.sendRichMessage("<green>Adicionado com sucesso!")
    }

    @Subcommand("untrust")
    fun onUntrust(sender: Player, targetName: String) {
        val island = ownedIslandOf(sender) ?: return

        if (sender.name.equals(targetName, true)) {
            sender.sendRichMessage("<red>Você quer se remover? Não entendi.")
            return
        }

        if (targetName.lowercase() !in island.trustedNames) {
            sender.sendRichMessage("<red>Este jogador não está adicionado em sua ilha.")
            return
        }

        island.trustedNames -= targetName.lowercase()
        island.markDirty()

        sender.sendRichMessage("<green>Removido com sucesso!")
    }

    @Subcommand("convidar|member")
    fun onMember(sender: Player, targetOnline: OnlinePlayer) {
        val island = ownedIslandOf(sender) ?: return
        IslandMembership.invite(sender, island, targetOnline.player)
    }
}
