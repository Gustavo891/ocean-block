package one_block.system.islands.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import me.devnatan.inventoryframework.ViewFrame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandMember
import one_block.system.islands.island.IslandMemberPermission
import one_block.system.islands.island.IslandUpgrade
import one_block.system.islands.phase.PhaseManager
import one_block.system.islands.view.IslandView
import one_block.system.islands.view.IslandsView
import one_block.system.islands.view.PhaseView
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandAlias("island|is")
class IslandCommand(
    private val viewFrame: ViewFrame,
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player, @Optional targetName: String?) {

        if(targetName != null) {
            val playerIsland = IslandManager.getIslandByOwner(targetName)
            if(playerIsland == null) {
                sender.sendRichMessage("<red>Esse jogador não tem uma ilha!")
                return
            }
            if(playerIsland.isMember(sender.name) || playerIsland.isTrustedMember(sender.name)) {
                playerIsland.teleportTo(sender)
                return
            }
        }

        var island = IslandManager.getIslandByOwner(sender.name)
        if (island == null) {
            sender.sendRichMessage("<green>Criando sua ilha...")
            island = IslandManager.createIsland(sender)
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
        val spawn = island.spawn.toBukkit()

        if (spawn == null) {
            sender.sendRichMessage("<red>Erro: mundo não carregado.")
            return
        }

        sender.teleport(spawn.add(0.0, 2.0, 0.0))
        //viewFrame.open(IslandView::class.java, sender)

    }

    @Subcommand("menu")
    fun onMenu(sender: Player) {
        val islandAt = IslandManager.getIslandByPlayer(sender.uniqueId)
        if(islandAt == null) {
            sender.sendRichMessage("<red>Você não está em nenhuma ilha.")
            return
        }
        viewFrame.open(IslandView::class.java, sender)
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
    fun onTrust(sender: Player,  targetOnline: OnlinePlayer) {
        val target = targetOnline.player

        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Você não tem ilha!")
            return
        }

        if (island.ownerUuid != sender.uniqueId) {
            sender.sendRichMessage("<red>Você não é o dono desta ilha!")
            return
        }

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
    fun onUntrust(sender: Player,  targetName: String) {
        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Você não tem ilha!")
            return
        }

        if (island.ownerUuid != sender.uniqueId) {
            sender.sendRichMessage("<red>Você não é o dono desta ilha!")
            return
        }

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

        val island = IslandManager.getIslandByPlayer(sender.uniqueId)
        if (island == null) {
            sender.sendRichMessage("<red>Você não tem ilha!")
            return
        }

        if (island.ownerUuid != sender.uniqueId) {
            sender.sendRichMessage("<red>Você não é o dono desta ilha!")
            return
        }

        if (sender.name.equals(target.name, true)) {
            sender.sendRichMessage("<red>Você quer se adicionar? Não entendi.")
            return
        }

       if ( island.members.size >= (island.getUpgradeLevel(IslandUpgrade.Members, 1) * 3)){
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