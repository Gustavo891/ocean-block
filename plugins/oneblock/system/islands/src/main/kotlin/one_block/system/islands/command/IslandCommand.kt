package one_block.system.islands.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandMember
import one_block.system.islands.island.IslandUpgrade
import one_block.system.islands.phase.PhaseAdvancement
import one_block.system.islands.phase.PhaseManager
import one_block.system.islands.reset.IslandResetService
import one_block.system.islands.view.IslandView
import one_block.system.islands.view.IslandsView
import one_block.system.islands.view.PhaseView
import org.bukkit.entity.Player

@CommandAlias("island|is")
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
            val playerIsland = IslandManager.getIslandByOwnerName(targetName.lowercase())
            if (playerIsland == null) {
                sender.sendRichMessage("<red>Esse jogador não tem uma ilha!")
                return
            }
            if (playerIsland.isMember(sender.name) || playerIsland.isTrustedMember(sender.name)) {
                playerIsland.teleportTo(sender)
                return
            }
        }

        var island = IslandManager.getIslandByOwnerUUID(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<green>Criando sua ilha...")
            island = IslandManager.createIsland(sender)

            if (island == null) {
                sender.sendRichMessage("<red>As ilhas não foram carregadas. Avise a staff antes de continuar.")
                return
            }

            sender.sendRichMessage("<green>Sua ilha foi criada com sucesso!")
            val spawn = island.spawn.toBukkit()

            if (spawn == null) {
                sender.sendRichMessage("<red>Erro: mundo não carregado.")
                return
            }

            sender.teleport(spawn.add(0.0, 2.0, 0.0))
            sender.fallDistance = 0f
            island.onJoin(sender)

            sender.sendRichMessage("<green>Teleportado para sua ilha!")
            return
        }
        island.teleportTo(sender)

    }

    @Subcommand("menu")
    fun onMenu(sender: Player) {
        val islandAt = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (islandAt == null) {
            sender.sendRichMessage("<red>Você não está em nenhuma ilha.")
            return
        }
        viewFrame.open(IslandView::class.java, sender)
    }

    @Subcommand("remove")
    fun onRemove(sender: Player, targetName: String) {
        val island = ownedIslandOf(sender) ?: return

        if (!island.members.contains(targetName.lowercase())) {
            sender.sendRichMessage("<red>Esse jogador não faz parte de sua ilha.")
            return
        }

        island.members.remove(targetName.lowercase())
        island.markDirty()
        sender.sendRichMessage("<green>O jogador <white>${targetName} <green>foi removido da sua ilha.")
    }

    @Subcommand("undeny")
    fun unBlacklist(sender: Player, target: String) {

        val island = ownedIslandOf(sender) ?: return

        if (target.equals(island.ownerName, true)) {
            sender.sendRichMessage("<red>Você não pode desbloquear você mesmo.")
            return
        }

        if (!island.blackListNames().contains(target)) {
            sender.sendRichMessage("<red>Esse jogador não está bloqueado de acessar sua ilha.")
            return
        }

        island.removeBlacklist(target)
        island.markDirty()
        sender.sendRichMessage("<green>O jogador <white>${target} <green>foi liberado de acessar sua ilha.")
    }

    @Subcommand("deny")
    fun blackList(sender: Player, target: OnlinePlayer) {
        val island = ownedIslandOf(sender) ?: return

        if (island.ownerUuid == target.player.uniqueId) {
            sender.sendRichMessage("<red>Você não pode bloquear você mesmo.")
            return
        }

        if (island.members.contains(target.player.name) || island.trustedNames.contains(target.player.name)) {
            sender.sendRichMessage("<red>Você não pode bloquear um membro/confiavel de sua ilha. Remova-o primeiro!")
            return
        }

        if (island.blackListNames().contains(target.player.name)) {
            sender.sendRichMessage("<red>Esse jogador já está bloqueado de acessar sua ilha.")
            return
        }

        island.addBlacklist(target.player)
        island.markDirty()
        sender.sendRichMessage("<green>O jogador <white>${target.player.name} <green>foi bloqueado de acessar sua ilha.")

    }

    @Subcommand("visitar|v")
    fun onVisit(sender: Player, targetName: String) {
        val island = IslandManager.getIslandByOwnerName(targetName)
        if (island == null) {
            sender.sendRichMessage("<red>Este jogador não possui uma ilha!")
            return
        }

        if (island.blackListNames().contains(sender.name)) {
            sender.sendRichMessage("<red>Você está proibido de acessar esta ilha.")
            return
        }

        if (!island.isPublic) {
            sender.sendRichMessage("<red>A ilha deste jogador não é publica.")
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
        val island = IslandManager.getIslandByPlayer(sender.uniqueId) ?: return
        island.isPublic = isPublic
        IslandManager.markDirty(island.uuid)

        sender.sendRichMessage("<green>Alterado para ${isPublic}.")
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

        island.spawn = SimpleLocation.fromBukkit(sender.location)
        sender.sendRichMessage("<green>Seu home foi definido.")
    }

    @Subcommand("phases|fases")
    fun onPhases(sender: Player) {
        viewFrame.open(PhaseView::class.java, sender)
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

    @Subcommand("setphase|setfase")
    @CommandPermission("screen.dev")
    @CommandCompletion("@players @phases")
    fun onSetPhase(sender: Player, targetName: String, phaseId: String) {
        val island = IslandManager.getIslandByOwnerName(targetName.lowercase())
        if (island == null) {
            sender.sendRichMessage("<red>Esse jogador não tem uma ilha!")
            return
        }

        val phase = PhaseManager.get(phaseId)
        if (phase == null) {
            val ids = PhaseManager.phases().joinToString(", ") { it.id }
            sender.sendRichMessage("<red>Fase desconhecida: <white>$phaseId")
            sender.sendRichMessage("<gray>Disponíveis: <white>$ids")
            return
        }

        val anterior = island.getCurrentPhase()
        PhaseAdvancement.setPhase(island, phase)

        val de = anterior?.prettyName ?: island.currentPhaseId
        sender.sendRichMessage(
            "<green>Ilha de <white>$targetName<green> movida de <white>$de<green> para <white>${phase.prettyName}<green>."
        )
        sender.sendRichMessage("<gray>Progresso zerado. Nenhuma recompensa foi entregue.")

        PhaseAdvancement.onlineMembers(island).forEach {
            it.sendRichMessage("<yellow>A fase da sua ilha foi alterada para <white>${phase.prettyName}<yellow>.")
        }
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

    @Subcommand("member")
    fun onMember(sender: Player, targetOnline: OnlinePlayer) {
        val target = targetOnline.player

        val island = ownedIslandOf(sender) ?: return

        if (sender.name.equals(target.name, true)) {
            sender.sendRichMessage("<red>Você quer se adicionar? Não entendi.")
            return
        }

        if (island.members.size >= ((island.getUpgradeLevel(IslandUpgrade.Members) + 1) * 3)) {
            sender.sendRichMessage("<red>Sua ilha está lotada de membros.")
            return
        }

        if (!island.addMember(target.name, IslandMember(target.name, mutableSetOf()))) {
            sender.sendRichMessage("<green>Este jogador já está adicionado")
            return
        }

        sender.sendRichMessage("<green>${target.name} foi adicionado como membro!")
    }
}
