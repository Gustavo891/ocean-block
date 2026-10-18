package one_block.system.minions.listener

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object TerminatorKill {

    const val CLAIM_WINDOW_TICKS = 60

    private val MINION_KEY = NamespacedKey("oneblock", "terminator_minion")
    private val TICK_KEY = NamespacedKey("oneblock", "terminator_tick")

    fun mark(entity: LivingEntity, minionUuid: UUID) {
        entity.persistentDataContainer.set(MINION_KEY, PersistentDataType.STRING, minionUuid.toString())
        entity.persistentDataContainer.set(TICK_KEY, PersistentDataType.INTEGER, Bukkit.getCurrentTick())
    }

    fun claimant(entity: LivingEntity): UUID? {
        val container = entity.persistentDataContainer
        val raw = container.get(MINION_KEY, PersistentDataType.STRING) ?: return null
        val tick = container.get(TICK_KEY, PersistentDataType.INTEGER) ?: return null

        if (Bukkit.getCurrentTick() - tick > CLAIM_WINDOW_TICKS) return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }
}
