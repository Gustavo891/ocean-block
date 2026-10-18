package one_block.system.spawners.manager

import one_block.system.spawners.models.LootRoll
import one_block.system.spawners.rules.SpawnRules
import org.bukkit.Particle
import org.bukkit.SoundCategory
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.UUID

object SpawnerKill {

    private const val DEATH_PARTICLES = 12
    private const val DEATH_SPREAD = 0.35

    fun isLethal(entity: LivingEntity, finalDamage: Double): Boolean =
        finalDamage >= entity.health

    fun apply(entity: LivingEntity, ownerUuid: UUID, killer: Player, sweep: Boolean = false): Boolean {
        val placed = PlacedSpawnerManager.get(ownerUuid) ?: return false
        val type = placed.type() ?: return false
        if (placed.liveMobs <= 0) return false

        val kills = if (sweep) placed.liveMobs else 1
        placed.liveMobs = SpawnRules.afterKills(placed.liveMobs, kills)
        PlacedSpawnerManager.markDirty(placed)

        val world = entity.world
        val location = entity.location
        LootRoll.rollMany(type.loot, kills).forEach { drop ->
            killer.inventory.addItem(drop).values.forEach { leftover ->
                killer.world.dropItemNaturally(killer.location, leftover)
            }
        }

        killer.giveExp(type.xpPerKill)

        if (placed.liveMobs <= 0) {
            entity.isSilent = false
            return true
        }

        playDeathEffects(entity)
        heal(entity)
        SpawnerEntity.refreshName(entity, placed, type)
        return false
    }

    private fun playDeathEffects(entity: LivingEntity) {
        val world = entity.world
        val center = entity.location.add(0.0, entity.height / 2.0, 0.0)

        world.spawnParticle(Particle.POOF, center, DEATH_PARTICLES, DEATH_SPREAD, DEATH_SPREAD, DEATH_SPREAD, 0.0)

        val deathSound = entity.deathSound ?: return
        world.playSound(entity.location, deathSound, SoundCategory.HOSTILE, 1.0f, 1.0f)
    }

    private fun heal(entity: LivingEntity) {
        val max = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        entity.health = max
    }
}
