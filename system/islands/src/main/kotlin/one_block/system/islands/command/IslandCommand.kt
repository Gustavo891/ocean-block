package one_block.system.islands.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import me.devnatan.inventoryframework.ViewFrame
import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandMember
import one_block.system.islands.phase.PhaseManager
import one_block.system.islands.view.IslandsView
import one_block.system.islands.view.PhaseView
import org.bukkit.entity.Player

@CommandAlias("island|is")
class IslandCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        var island = IslandManager.islandsById.values.find { it.ownerUuid == sender.uniqueId }

        if (island == null) {
            sender.sendRichMessage("<green>Criando sua ilha...")
            island = IslandManager.createIsland(sender)
            sender.sendRichMessage("<green>Sua ilha foi criada com sucesso!")
        }

        val spawn = island.spawn.toBukkit()

        if (spawn == null) {
            sender.sendRichMessage("<red>Erro: mundo não carregado.")
            return
        }

        sender.teleport(spawn.add(0.0, 2.0, 0.0))
        sender.fallDistance = 0f
        island.onJoin(sender)

        sender.sendRichMessage("<green>Teleportado para sua ilha!")
    }

    @Subcommand("visitar|v")
    fun onVisit(sender: Player, targetName: String) {
        val island = IslandManager.islandsById.values.find { it.ownerName.equals(targetName, true) }
        if (island == null) {
            sender.sendRichMessage("<red>Este jogador não possui uma ilha!")
            return
        }

        if (!island.isPublic) {
            sender.sendRichMessage("<red>A ilha deste jogador não é publica.")
            return
        }

        sender.teleport(island.spawn.toBukkit()!!.add(0.0, 2.0, 0.0))
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

    @Subcommand("phases")
    fun onPhases(sender: Player) {
        viewFrame.open(PhaseView::class.java, sender)
    }

    @Subcommand("trust")
    fun onTrust(sender: Player, targetName: String) {
        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Você não tem ilha!")
            return
        }

        if (sender.name.equals(targetName, true)) {
            sender.sendRichMessage("<red>Você quer se adicionar? Não entendi.")
            return
        }

        if (!island.addTrustedMember(targetName)) {
            sender.sendRichMessage("<green>Este jogador já está adicionado")
            return
        }

        sender.sendRichMessage("<green>Adicionado com sucesso!")
    }

    @Subcommand("member")
    fun onMember(sender: Player, targetName: String) {
        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Você não tem ilha!")
            return
        }

        if (sender.name.equals(targetName, true)) {
            sender.sendRichMessage("<red>Você quer se adicionar? Não entendi.")
            return
        }

        if (!island.addMember(targetName, IslandMember(targetName, 0))) {
            sender.sendRichMessage("<green>Este jogador já está adicionado")
            return
        }

        sender.sendRichMessage("<green>Adicionado com sucesso!")
    }

    @Subcommand("reload")
    @CommandPermission("oneblock.admin")
    fun onReload(sender: Player) {
        PhaseManager.loadPhases()
        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }

    @Subcommand("all")
    @CommandPermission("oneblock.admin")
    fun onAll(sender: Player) {
        viewFrame.open(IslandsView::class.java, sender)
    }

    @Subcommand("test")
    fun onTest(sender: Player) {
        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Sem ilha!")
            return
        }

        island.refreshWorldBorder()
        island.onJoin(sender)
    }
}