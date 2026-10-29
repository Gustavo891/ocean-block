package ocean_block.system.islands.island

import ocean_block.libs.coordinate.SimpleLocation
import ocean_block.system.islands.phase.Phase
import ocean_block.system.islands.phase.PhaseManager
import ocean_block.system.islands.position.Position
import ocean_block.system.islands.island.type.IslandType
import org.bukkit.Bukkit
import org.bukkit.WorldBorder
import org.bukkit.entity.Player
import java.util.*

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
    val pendingRewardPhases: MutableList<String> = mutableListOf(),
    var size: Int = 50,
    var isPublic: Boolean,
    var settings: MutableMap<String, Boolean>? = null,
    var tipo: String = IslandType.PADRAO.id,
) {

    companion object {
        val BORDER_SIZE = listOf(50, 70, 100, 150)

        const val MEMBERS_PER_LEVEL = 3
    }

    val invites = mutableSetOf<String>()

    private val borderInstance by lazy { Bukkit.createWorldBorder() }

    fun onJoin(player: Player) {
        refreshWorldBorder()?.let { player.worldBorder = it }
    }

    fun getCurrentPhase(): Phase? {
        return PhaseManager.get(currentPhaseId)
    }

    fun refreshWorldBorder(): WorldBorder? {
        val bukkitCenter = center.toBukkit() ?: return null

        borderInstance.center = bukkitCenter
        borderInstance.size = borderSize().toDouble()
        borderInstance.warningDistance = -1
        return borderInstance
    }

    fun borderSize(): Int = BORDER_SIZE[borderLevel()]

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

    fun isTrustedMember(playerName: String): Boolean {
        return playerName.lowercase() in trustedNames
    }

    fun addTrustedMember(playerName: String): Boolean {
        return trustedNames.add(playerName.lowercase())
    }

    fun type(): IslandType = IslandType.fromId(tipo)

    fun maxMembers(): Int = (getUpgradeLevel(IslandUpgrade.Members) + 1) * MEMBERS_PER_LEVEL

    fun isBlocked(playerName: String): Boolean = playerName.lowercase() in blackListNames()

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

        if (isBlocked(player.name)) {
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
}
