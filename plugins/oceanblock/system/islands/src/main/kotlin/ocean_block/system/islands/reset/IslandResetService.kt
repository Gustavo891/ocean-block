package ocean_block.system.islands.reset

import ocean_block.system.islands.account.AccountReset
import ocean_block.system.islands.choice.IslandChoice
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.islands.island.IslandPlacement
import ocean_block.system.islands.island.StarterIsland
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import java.util.UUID

object IslandResetService {

    private const val DEFAULT_PHASE = "planicie"

    private val running = mutableSetOf<UUID>()

    fun previewTerrain(sender: CommandSender, island: Island) {
        val area = IslandArea.of(island)

        if (area == null) {
            sender.sendRichMessage("<red>Erro: mundo <white>${island.center.world}</white> não carregado.")
            return
        }

        val entities = area.world.entities.filter { it !is Player && area.contains(it.location) }
        val items = entities.count { it is Item }

        sender.sendRichMessage("<gold>Preview do reset de terreno de <white>${island.ownerName}</white>:")
        sender.sendRichMessage("<gray>- área: <white>${area.maxX - area.minX}x${area.maxZ - area.minZ}</white> em volta do centro")
        sender.sendRichMessage("<gray>- mobs: <white>${entities.size - items}</white>")
        sender.sendRichMessage("<gray>- itens no chão: <white>$items</white>")

        IslandResetHooks.describeAll(island, area).forEach {
            sender.sendRichMessage("<gray>- <white>$it</white>")
        }

        sender.sendRichMessage("<yellow>Nada foi alterado. Use <white>-confirm</white> para executar.")
        sender.sendRichMessage("<dark_gray>O progresso da ilha não é afetado por este comando.")
    }

    fun previewProgress(sender: CommandSender, island: Island) {
        val upgrades = IslandUpgrade.entries.joinToString(", ") {
            "${it.name}=${island.getUpgradeLevel(it)}"
        }

        sender.sendRichMessage("<gold>Preview do reset de progresso de <white>${island.ownerName}</white>:")
        sender.sendRichMessage("<gray>- fase: <white>${island.currentPhaseId}</white>")
        sender.sendRichMessage("<gray>- recompensas pendentes: <white>${island.pendingRewardPhases.size}</white>")
        sender.sendRichMessage("<gray>- membros: <white>${island.members.size}</white>")
        sender.sendRichMessage("<gray>- confiáveis: <white>${island.trustedNames.size}</white>")
        sender.sendRichMessage("<gray>- bloqueados: <white>${island.blackListNames().size}</white>")
        sender.sendRichMessage("<gray>- upgrades: <white>$upgrades</white>")

        sender.sendRichMessage("<yellow>Nada foi alterado. Use <white>-confirm</white> para executar.")
        sender.sendRichMessage("<dark_gray>Os blocos da ilha não são afetados por este comando.")
    }

    fun resetTerrain(sender: CommandSender, island: Island) {
        val area = IslandArea.of(island)

        if (area == null) {
            sender.sendRichMessage("<red>Erro: mundo <white>${island.center.world}</white> não carregado.")
            return
        }

        if (!running.add(island.uuid)) {
            sender.sendRichMessage("<red>Já existe um reset de terreno em andamento nessa ilha.")
            return
        }

        sender.sendRichMessage("<gray>Restaurando o mar da ilha de <white>${island.ownerName}</white>...")

        OceanRegen.regenerate(area) { ok ->
            running.remove(island.uuid)

            if (!ok) {
                sender.sendRichMessage("<red>Falha ao restaurar o mar da ilha de <white>${island.ownerName}</white>. Nada foi alterado.")
                return@regenerate
            }

            IslandResetHooks.fireTerrainReset(island, area)

            area.world.entities
                .filter { it !is Player && area.contains(it.location) }
                .forEach { it.remove() }

            island.spawn = IslandPlacement.spawn(island.position, island.center.world)
            island.markDirty()

            StarterIsland.generate(island) {
                returnPlayers(island, area)
                sender.sendRichMessage("<green>Ilha de <white>${island.ownerName}</white> resetada.")
            }
        }
    }

    fun previewResetPlayer(sender: CommandSender, name: String, island: Island?) {
        sender.sendRichMessage("<gold>Preview do resetplayer de <white>$name</white>:")
        if (island != null) {
            sender.sendRichMessage("<gray>- ilha <white>${island.type().nome}</white> apagada, o lote volta a ser mar")
            sender.sendRichMessage("<gray>- membros expulsos: <white>${island.members.size}</white>")
        } else {
            sender.sendRichMessage("<gray>- sem ilha própria; sai das ilhas em que é membro")
        }
        sender.sendRichMessage("<gray>- conta zerada: nível, missões, recompensas e tempo jogado, passe, coleções, Coins, Pérolas, leilões, preferências")
        sender.sendRichMessage("<gray>- inventário, ender chest, XP, vida, fome e efeitos limpos; recebe o kit inicial de novo")
        sender.sendRichMessage("<gray>- escolhe o tipo de ilha de novo (agora, se estiver online, senão no próximo login)")
        sender.sendRichMessage("<yellow>Nada foi alterado. Use <white>-confirm</white> para executar.")
    }

    fun resetPlayer(sender: CommandSender, uuid: UUID, name: String) {
        IslandManager.getIslandByOwnerUUID(uuid)?.let {
            deleteIsland(sender, it)
            return
        }

        IslandManager.islandsById.values.filter { it.isMember(name) }.forEach { island ->
            island.members.remove(name.lowercase())
            island.markDirty()
        }
        IslandManager.playerIsland.remove(uuid)
        resetAccount(uuid)
        sender.sendRichMessage("<green>Conta de <white>$name</white> resetada.")
    }

    private fun resetAccount(uuid: UUID) {
        AccountReset.reset(uuid)
        Bukkit.getPlayer(uuid)?.let { IslandChoice.begin(it) }
    }

    fun deleteIsland(sender: CommandSender, island: Island) {
        val area = IslandArea.of(island)

        if (area == null) {
            sender.sendRichMessage("<red>Erro: mundo <white>${island.center.world}</white> não carregado.")
            return
        }

        if (!running.add(island.uuid)) {
            sender.sendRichMessage("<red>Já existe um reset em andamento nessa ilha.")
            return
        }

        sender.sendRichMessage("<gray>Apagando a ilha de <white>${island.ownerName}</white>...")

        OceanRegen.regenerate(area) { ok ->
            running.remove(island.uuid)

            if (!ok) {
                sender.sendRichMessage("<red>Falha ao restaurar o mar da ilha de <white>${island.ownerName}</white>. Nada foi alterado.")
                return@regenerate
            }

            IslandResetHooks.fireTerrainReset(island, area)

            area.world.entities
                .filter { it !is Player && area.contains(it.location) }
                .forEach { it.remove() }

            val visitors = area.world.players.filter { area.contains(it.location) }
            expelMembers(island)
            IslandManager.deleteIsland(island)

            resetAccount(island.ownerUuid)
            visitors.filter { it.uniqueId != island.ownerUuid && it.isOnline }.forEach { visitor ->
                IslandManager.getHomeIsland(visitor)?.teleportTo(visitor) ?: IslandChoice.begin(visitor)
            }

            sender.sendRichMessage("<green>Ilha de <white>${island.ownerName}</white> apagada e conta resetada. O dono vai escolher o tipo de novo.")
        }
    }

    fun resetProgress(sender: CommandSender, island: Island) {
        island.currentPhaseId = DEFAULT_PHASE
        island.pendingRewardPhases.clear()
        island.upgrades.clear()
        island.size = Island.BORDER_SIZE.first()
        island.members.clear()
        island.trustedNames.clear()
        island.blackList = null
        island.settings = null
        island.isPublic = false
        island.spawn = IslandPlacement.spawn(island.position, island.center.world)
        island.refreshWorldBorder()

        expelMembers(island)

        Bukkit.getPlayer(island.ownerUuid)?.let { island.onJoin(it) }

        island.markDirty()

        sender.sendRichMessage("<green>Progresso de <white>${island.ownerName}</white> resetado.")
    }

    private fun expelMembers(island: Island) {
        val expelled = IslandManager.playerIsland
            .filter { (playerUuid, islandUuid) -> islandUuid == island.uuid && playerUuid != island.ownerUuid }
            .keys
            .toList()

        expelled.forEach { playerUuid ->
            IslandManager.playerIsland.remove(playerUuid)

            val player = Bukkit.getPlayer(playerUuid) ?: return@forEach
            if (IslandManager.getIslandAt(player.location)?.uuid != island.uuid) return@forEach

            player.teleport(player.world.spawnLocation)
            player.sendRichMessage("<red>A ilha em que você estava foi resetada.")
        }
    }

    private fun returnPlayers(island: Island, area: IslandArea) {
        island.getIslandOnlinePlayers()
            .filter { area.contains(it.location) }
            .forEach { island.teleportTo(it) }
    }
}
