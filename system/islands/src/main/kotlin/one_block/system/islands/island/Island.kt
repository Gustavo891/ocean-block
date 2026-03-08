package one_block.system.islands.island

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.coordinate.SimpleLocation
import one_block.system.islands.bossbar.BossbarManager
import one_block.system.islands.ext.getRandomByWeight
import one_block.system.islands.hud.HudManager
import one_block.system.islands.phase.Phase
import one_block.system.islands.phase.PhaseManager
import one_block.system.islands.position.Position
import one_block.system.islands.util.MonsterParty
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
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
    var currentPhaseId: String = "origin",
    var grid: Int = 0,
    var size: Int = 30,
    var isPublic: Boolean,
) {

    companion object {
        val BORDER_SIZE = listOf(30, 50, 70, 100)
    }

    val invites = mutableSetOf<String>()

    private val borderInstance by lazy { Bukkit.createWorldBorder() }
    var currentWorldBorder = getWorldBorder()

    fun onJoin(player: Player) {
        player.worldBorder = currentWorldBorder
        BossbarManager.updatePlayerBar(player, this)
    }

    fun getCurrentPhase(): Phase? {
        return PhaseManager.get(currentPhaseId)
    }

    fun refreshWorldBorder() {
        currentWorldBorder = getWorldBorder()
    }

    fun addBlacklist(target: Player) {
        if (blackList == null) {
            blackList = mutableSetOf()
        }

        blackList!!.add(target.name.lowercase())
        val targetIsland = IslandManager.playerIsland[target.uniqueId]
        if (targetIsland == this.uuid) {
            val island = IslandManager.getIslandByOwnerUUID(target.uniqueId)
            island?.teleportTo(target)
        }
    }

    fun removeBlacklist(playerName: String): Boolean {
        if (blackList == null) {
            blackList = mutableSetOf()
        }

        return blackList!!.remove(playerName.lowercase())
    }

    fun processBlockBreak(player: Player, block: Block) {
        val phase = getCurrentPhase() ?: return

        handleMobSpawn(phase, block)
        handleMonsterParty(phase, block)

        val nextMaterial = phase.blockPool.getRandomByWeight() ?: Material.GRASS_BLOCK
        block.type = nextMaterial
        player.damageItemStack(player.inventory.itemInMainHand, 1)

        grid++
        if (grid >= phase.blocksToNextPhase) advancePhase(player)
    }

    fun advancePhase(player: Player) {
        val currentPhase = getCurrentPhase() ?: return
        val nextPhase = PhaseManager.getNextPhase(currentPhase.priority)

        if (nextPhase != null) {
            this.currentPhaseId = nextPhase.id
            this.grid = 0

            val content = Component.text {
                it.appendNewline()
                it.append(MiniMessage.miniMessage().deserialize("<green>Nova fase desbloqueada!").decoration(TextDecoration.ITALIC, false))
                it.appendNewline()
            }

            val players = Bukkit.getOnlinePlayers().filter { IslandManager.playerIsland[it.uniqueId] == this.uuid }
            players.forEach { it.sendMessage(content) }
        }

        this.grid = 0
    }

    fun isTrustedMember(playerName: String): Boolean {
        return playerName.lowercase() in trustedNames
    }

    fun addTrustedMember(playerName: String): Boolean {
        return trustedNames.add(playerName.lowercase())
    }

    fun getMaxMembers(size: Int): Int {
        return (size * 3)
    }

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

    fun isTrustAllowed(island: Island, player: Player): Boolean {
        if (player.hasPermission("screen.dev")) return true
        if (island.ownerUuid == player.uniqueId) return true

        val islandMember = island.members[player.name.lowercase()]
        if (islandMember != null) return true

        if (!island.isTrustedMember(player.name)) {
            player.sendRichMessage("<red>Você não faz parte dessa ilha")
            return false
        }

        val ownerPlayer = Bukkit.getPlayer(island.ownerUuid)
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

    private fun handleMobSpawn(phase: Phase, block: Block) {
        if (!chance(2.0)) return

        val mobType = phase.mobPool.mapValues { it.value.weight }.getRandomByWeight() ?: return
        val data = phase.mobPool[mobType] ?: return
        val amount = (data.min..data.max).random()

        for (i in 1..amount) {
            block.world.spawnEntity(center.toBukkit()!!.clone().add(0.5, 1.0, 0.5), mobType)
        }
    }

    private fun handleMonsterParty(phase: Phase, block: Block) {
        if (!chance(1.0)) return

        val monsterParty = phase.monsterParty ?: return
        val monsterConfig = monsterParty.random()

        repeat((monsterConfig.min..monsterConfig.max).random()) {
            MonsterParty.spawnConfigureMob(block.location, monsterConfig)
        }

        // notifica os jogadores daquela ilha que começou uma invasão

    }

    fun upgradeBorderSize() {
        val currentSize = getUpgradeLevel(IslandUpgrade.Size)
        val newSize = currentSize + 1
        setUpgradeLevel(IslandUpgrade.Size, newSize)
        size = BORDER_SIZE[newSize]
        refreshWorldBorder()
    }

    private fun getWorldBorder(): WorldBorder {
        val curentSize = getUpgradeLevel(IslandUpgrade.Size)
        val borderAmount = BORDER_SIZE[curentSize]

        borderInstance.center = center.toBukkit()!!
        borderInstance.size = borderAmount.toDouble()
        borderInstance.warningDistance = -1
        return borderInstance
    }

    fun getIslandOnlinePlayers(): List<Player> {
        val memberPlayers = IslandManager.playerIsland
            .filter { (_, islandId) -> islandId == uuid }
            .mapNotNull { (playerUuid, _) -> Bukkit.getPlayer(playerUuid) }
            .filter { it.isOnline }

        val owner = Bukkit.getPlayer(ownerUuid)

        return (memberPlayers + listOfNotNull(owner)).distinctBy { it.uniqueId }
    }

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

    fun blackListNames(): MutableSet<String> {
        if (blackList == null) {
            blackList = mutableSetOf()
        }

        return blackList!!
    }

    private fun chance(percent: Double): Boolean {
        return ThreadLocalRandom.current().nextDouble(100.0) < percent
    }
}
