package one_block.system.islands.island

import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.hud.IslandHud
import one_block.system.islands.ext.getRandomByWeight
import one_block.system.islands.phase.MOB_SPAWN_CHANCE_PERCENT
import one_block.system.islands.phase.MONSTER_PARTY_CHANCE_PERCENT
import one_block.system.islands.phase.Phase
import one_block.system.islands.phase.PhaseAdvancement
import one_block.system.islands.phase.PhaseBlocks
import one_block.system.islands.phase.PhaseManager
import one_block.system.islands.phase.PhaseProgress
import one_block.system.islands.position.Position
import one_block.system.islands.util.MonsterParty
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.WorldBorder
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ThreadLocalRandom

data class Island(
    val uuid: UUID,
    var ownerUuid: UUID,
    var ownerName: String,
    val center: SimpleLocation,
    val position: Position,
    var spawn: SimpleLocation,
    val trustedNames: MutableSet<String> = mutableSetOf(),
    var blackList: MutableSet<String>? = null,
    val members: MutableMap<String, IslandMember> = mutableMapOf(),
    val upgrades: MutableMap<String, Int> = mutableMapOf(),
    var currentPhaseId: String = "planicie",
    var grid: Int = 0,
    val pendingRewardPhases: MutableList<String> = mutableListOf(),
    var size: Int = 30,
    var isPublic: Boolean,
    var settings: MutableMap<String, Boolean>? = null,
) {

    companion object {
        val BORDER_SIZE = listOf(30, 50, 70, 100)
    }

    val invites = mutableSetOf<String>()

    private val borderInstance by lazy { Bukkit.createWorldBorder() }

    fun onJoin(player: Player) {
        refreshWorldBorder()?.let { player.worldBorder = it }
        IslandHud.show(player, this)
    }

    fun getCurrentPhase(): Phase? {
        return PhaseManager.get(currentPhaseId)
    }

    fun refreshWorldBorder(): WorldBorder? {
        val bukkitCenter = center.toBukkit() ?: return null

        borderInstance.center = bukkitCenter
        borderInstance.size = BORDER_SIZE[borderLevel()].toDouble()
        borderInstance.warningDistance = -1
        return borderInstance
    }

    private fun borderLevel(): Int =
        getUpgradeLevel(IslandUpgrade.Size).coerceIn(BORDER_SIZE.indices)

    fun addBlacklist(target: Player) {
        blackListNames().add(target.name.lowercase())

        val targetIsland = IslandManager.playerIsland[target.uniqueId]
        if (targetIsland == this.uuid) {
            IslandManager.getIslandByOwnerUUID(target.uniqueId)?.teleportTo(target)
        }
    }

    fun removeBlacklist(playerName: String): Boolean = blackListNames().remove(playerName.lowercase())

    fun processBlockBreak(block: Block) {
        val phase = getCurrentPhase() ?: return

        handleMobSpawn(phase, block)
        handleMonsterParty(phase, block)

        PhaseBlocks.place(block, phase, Material.GRASS_BLOCK)

        val target = phase.blocksToNextPhase
        val before = grid
        grid = PhaseProgress.increment(before, target)

        if (!PhaseProgress.isComplete(grid, target)) return

        val crossedNow = PhaseProgress.crossedThreshold(before, grid, target)
        val hasNext = PhaseManager.getNextPhase(phase.priority) != null

        if (crossedNow || hasNext) advancePhase()
    }

    fun advancePhase() {
        PhaseAdvancement.advance(this)
    }

    fun isTrustedMember(playerName: String): Boolean {
        return playerName.lowercase() in trustedNames
    }

    fun addTrustedMember(playerName: String): Boolean {
        return trustedNames.add(playerName.lowercase())
    }

    fun getMaxMembers(upgradeLevel: Int): Int = upgradeLevel * 3

    fun isMember(playerName: String): Boolean {
        return playerName.lowercase() in members.keys
    }

    fun addMember(playerName: String, member: IslandMember): Boolean {
        return members.put(playerName.lowercase(), member) == null
    }

    fun setUpgradeLevel(upgrade: IslandUpgrade, level: Int) {
        upgrades[upgrade.name.lowercase()] = level
    }

    fun getUpgradeLevel(upgrade: IslandUpgrade, def: Int = 0) = upgrades.getOrDefault(upgrade.name.lowercase(), def)

    fun isTrustAllowed(player: Player): Boolean {
        if (player.hasPermission("screen.dev")) return true
        if (ownerUuid == player.uniqueId) return true

        if (members[player.name.lowercase()] != null) return true

        if (!isTrustedMember(player.name)) {
            player.sendRichMessage("<red>Você não faz parte dessa ilha")
            return false
        }

        val ownerPlayer = Bukkit.getPlayer(ownerUuid)
        return if (ownerPlayer == null || !ownerPlayer.isOnline) {
            player.sendRichMessage("<red>Você só pode fazer ações enquanto o dono estiver online.")
            false
        } else {
            true
        }
    }

    fun markDirty() {
        IslandManager.markDirty(uuid)
    }

    fun isSettingEnabled(setting: IslandSetting): Boolean =
        IslandSettings.isEnabled(settings, setting)

    fun toggleSetting(setting: IslandSetting): Boolean {
        val newValue = !isSettingEnabled(setting)
        val map = settings ?: mutableMapOf<String, Boolean>().also { settings = it }
        map[setting.name] = newValue
        markDirty()
        return newValue
    }

    private fun handleMobSpawn(phase: Phase, block: Block) {
        if (!isSettingEnabled(IslandSetting.MobSpawn)) return
        if (!chance(MOB_SPAWN_CHANCE_PERCENT)) return

        val mobType = phase.mobPool.mapValues { it.value.weight }.getRandomByWeight() ?: return
        val data = phase.mobPool[mobType] ?: return
        val spawnAt = center.toBukkit()?.clone()?.add(0.5, 1.0, 0.5) ?: return

        repeat((data.min..data.max).random()) {
            block.world.spawnEntity(spawnAt, mobType)
        }
    }

    private fun handleMonsterParty(phase: Phase, block: Block) {
        if (!chance(MONSTER_PARTY_CHANCE_PERCENT)) return

        val monsterParty = phase.monsterParty ?: return
        val monsterConfig = monsterParty.associateWith { it.weight }.getRandomByWeight() ?: return

        repeat((monsterConfig.min..monsterConfig.max).random()) {
            MonsterParty.spawnConfigureMob(block.location, monsterConfig)
        }
    }

    fun upgradeBorderSize() {
        val newLevel = (getUpgradeLevel(IslandUpgrade.Size) + 1).coerceIn(BORDER_SIZE.indices)
        setUpgradeLevel(IslandUpgrade.Size, newLevel)
        size = BORDER_SIZE[newLevel]
        refreshWorldBorder()
    }

    fun getIslandOnlinePlayers(): List<Player> = Bukkit.getOnlinePlayers()
        .filter { it.uniqueId == ownerUuid || IslandManager.playerIsland[it.uniqueId] == uuid }

    fun teleportTo(player: Player) {
        val spawn = spawn.toBukkit()

        if (blackListNames().contains(player.name)) {
            player.sendRichMessage("<red>Você está bloqueado de acessar essa ilha.")
            return
        }

        if (spawn == null) {
            player.sendRichMessage("<red>Erro: mundo não carregado.")
            return
        }
        onJoin(player)
        player.teleport(spawn.add(0.0, 2.0, 0.0))
    }

    fun blackListNames(): MutableSet<String> =
        blackList ?: mutableSetOf<String>().also { blackList = it }

    private fun chance(percent: Double): Boolean {
        return ThreadLocalRandom.current().nextDouble(100.0) < percent
    }
}
