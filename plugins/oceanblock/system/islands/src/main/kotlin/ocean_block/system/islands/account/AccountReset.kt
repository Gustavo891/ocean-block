package ocean_block.system.islands.account

import ocean_block.libs.account.AccountResetEvent
import ocean_block.libs.instance.Instance
import ocean_block.libs.instance.ScheduledExecutor
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.util.UUID

object AccountReset {

    private const val FULL_FOOD = 20
    private const val FULL_SATURATION = 5f
    private const val DEFAULT_HEALTH = 20.0

    private val pending = PendingResets()
    private val repository by lazy { PendingResetRepository() }

    fun load() {
        runCatching { repository.toList() }
            .onSuccess { pending.load(it) }
            .onFailure { Instance.logger.severe("[conta] Falha ao carregar os resets pendentes: ${it.message}") }
    }

    fun reset(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            apply(player)
            return
        }
        if (!pending.add(uuid)) return
        ScheduledExecutor.execute {
            runCatching { repository.add(uuid) }
                .onFailure { Instance.logger.severe("[conta] Falha ao guardar o reset pendente de $uuid: ${it.message}") }
        }
    }

    fun consume(player: Player): Boolean {
        if (!pending.consume(player.uniqueId)) return false
        val uuid = player.uniqueId
        ScheduledExecutor.execute {
            runCatching { repository.remove(uuid) }
                .onFailure { Instance.logger.severe("[conta] Falha ao apagar o reset pendente de $uuid: ${it.message}") }
        }
        apply(player)
        return true
    }

    private fun apply(player: Player) {
        wipe(player)
        Bukkit.getPluginManager().callEvent(AccountResetEvent(player))
        Instance.logger.info("[conta] Conta de ${player.name} resetada.")
    }

    private fun wipe(player: Player) {
        player.inventory.clear()
        player.enderChest.clear()
        player.setItemOnCursor(null)
        player.totalExperience = 0
        player.level = 0
        player.exp = 0f
        player.health = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: DEFAULT_HEALTH
        player.foodLevel = FULL_FOOD
        player.saturation = FULL_SATURATION
        player.exhaustion = 0f
        player.fireTicks = 0
        player.fallDistance = 0f
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
        player.setRespawnLocation(null)
    }
}
