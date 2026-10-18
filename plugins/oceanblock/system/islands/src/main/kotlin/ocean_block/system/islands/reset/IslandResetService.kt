package ocean_block.system.islands.reset

import ocean_block.system.islands.hud.IslandHud
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.islands.island.RaftPlacement
import ocean_block.system.islands.island.StarterIsland
import ocean_block.system.islands.phase.PhaseAdvancement
import ocean_block.system.islands.phase.PhaseBlocks
import org.bukkit.Bukkit
import org.bukkit.Material
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
        sender.sendRichMessage("<gray>- blocos: <white>${TerrainCleaner.count(area)}</white>")
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
        sender.sendRichMessage("<gray>- fase: <white>${island.currentPhaseId}</white> (grid ${island.grid})")
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

        IslandResetHooks.fireTerrainReset(island, area)

        area.world.entities
            .filter { it !is Player && area.contains(it.location) }
            .forEach { it.remove() }

        sender.sendRichMessage("<gray>Limpando o terreno de <white>${island.ownerName}</white>...")

        TerrainCleaner.clear(
            area,
            onProgress = { sender.sendRichMessage("<dark_gray>Reset de ${island.ownerName}: $it%") },
            onDone = { removed ->
                running.remove(island.uuid)
                restoreCenter(island)
                returnPlayers(island, area)
                sender.sendRichMessage("<green>Terreno de <white>${island.ownerName}</white> resetado. <gray>($removed blocos)")
            },
        )
    }

    fun resetProgress(sender: CommandSender, island: Island) {
        PhaseAdvancement.clear(island.uuid)

        island.currentPhaseId = DEFAULT_PHASE
        island.grid = 0
        island.pendingRewardPhases.clear()
        island.upgrades.clear()
        island.size = Island.BORDER_SIZE.first()
        island.members.clear()
        island.trustedNames.clear()
        island.blackList = null
        island.settings = null
        island.isPublic = false
        island.spawn = RaftPlacement.spawn(island.position, island.center.world)
        island.refreshWorldBorder()

        expelMembers(island)

        Bukkit.getPlayer(island.ownerUuid)?.let { island.onJoin(it) }

        IslandHud.refresh(island)
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

    private fun restoreCenter(island: Island) {
        StarterIsland.generate(island)

        val center = island.center.toBukkit() ?: return
        val phase = island.getCurrentPhase()

        center.world.getBlockAt(center.blockX, center.blockY - 1, center.blockZ).type = Material.BEDROCK

        if (phase == null) {
            center.block.type = Material.STONE
            return
        }

        PhaseBlocks.place(center.block, phase)
    }
}
