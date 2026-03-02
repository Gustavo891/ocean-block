package one_block.system.islands.island

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.instance.Instance
import one_block.system.islands.bossbar.BossbarManager
import one_block.system.islands.ext.getRandomByWeight
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
import kotlin.random.Random
import kotlin.random.nextInt

data class Island(
    val uuid: UUID,
    var ownerUuid: UUID,
    var ownerName: String,
    val center: SimpleLocation,
    val position: Position,
    var spawn: SimpleLocation,
    val trustedNames: MutableSet<String> = mutableSetOf(),
    val members: MutableMap<String, IslandMember> = mutableMapOf(),
    val upgrades: MutableMap<String, Int> = mutableMapOf(),
    var currentPhaseId: String = "origin",
    var grid: Int = 0,
    var size: Int = 30,
    var isPublic: Boolean,
) {

    val invites = mutableSetOf<String>()
    companion object {
        val BORDER_SIZE = listOf(30, 50, 70, 100)
    }

    private val borderInstance by lazy { Bukkit.createWorldBorder() }
    var currentWorldBorder = getWorldBorder()

    fun onJoin(player: Player) {
        player.worldBorder = currentWorldBorder
        if(BossbarManager.getBossBar(this) == null) {
            BossbarManager.initBossBar(this)
        }
        BossbarManager.getBossBar(this)?.addViewer(player)

    }

    fun getCurrentPhase(): Phase? {
        return PhaseManager.get(currentPhaseId)
    }

    fun refreshWorldBorder() {
        currentWorldBorder = getWorldBorder()
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

            player.apply {
                sendMessage("§a§lNOVA FASE! §fAgora você está em: ${nextPhase.prettyName}")
                playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f)
                sendTitle("§a§lNOVA FASE", nextPhase.prettyName, 10, 70, 20)
            }
        }

        this.grid = 0
    }

    fun isTrustedMember(playerName: String): Boolean {
        return playerName.lowercase() in trustedNames
    }

    fun addTrustedMember(playerName: String): Boolean {
        return trustedNames.add(playerName.lowercase())
    }

    fun isMember(playerName: String): Boolean {
        return playerName.lowercase() in members.keys
    }

    fun addMember(playerName: String, member: IslandMember): Boolean {
        return members.put(playerName.lowercase(), member) == null
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

    fun getBorderSize(): Int {
        return upgrades["size"] ?: 0
    }

    fun getCropLevel(): Int {
        return upgrades["crop"] ?: 0
    }

    fun upgradeBorderSize() {
        val currentSize = upgrades["size"] ?: 0
        val newSize = currentSize + 1
        upgrades["size"] = newSize
        refreshWorldBorder()
    }

    private fun getWorldBorder(): WorldBorder {
        val curentSize = upgrades["size"] ?:0
        val borderAmount = BORDER_SIZE[curentSize]

        borderInstance.center = center.toBukkit()!!
        borderInstance.size = borderAmount.toDouble()
        borderInstance.warningDistance = -1
        return borderInstance
    }

    fun teleportTo(player: Player) {
        val spawn = spawn.toBukkit()

        if (spawn == null) {
            player.sendRichMessage("<red>Erro: mundo não carregado.")
            return
        }

        player.sendRichMessage("<red>Esse jogador não tem uma ilha!")
        player.teleport(spawn.add(0.0, 2.0, 0.0))
    }

    private fun chance(percent: Double): Boolean {
        return ThreadLocalRandom.current().nextDouble(100.0) < percent
    }
}
