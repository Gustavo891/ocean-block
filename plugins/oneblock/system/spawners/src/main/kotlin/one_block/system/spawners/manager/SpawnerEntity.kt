package one_block.system.spawners.manager


import one_block.libs.formatter.Mini
import one_block.libs.instance.Instance
import one_block.system.spawners.SpawnersSystem
import one_block.system.spawners.models.PlacedSpawner
import one_block.system.spawners.models.SpawnerType
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object SpawnerEntity {

    private const val SEARCH_RADIUS = 2.0
    private val entityMap = mutableMapOf<UUID, LivingEntity>()

    private fun anchorOf(placed: PlacedSpawner): Location? =
        placed.location.toBukkit()?.add(2.0, 0.0, 0.5)

    fun ownerOf(entity: Entity): UUID? {
        val raw = entity.persistentDataContainer.get(SpawnersSystem.OWNER_SPAWNER, PersistentDataType.STRING)
            ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    fun find(placed: PlacedSpawner): LivingEntity? {
        entityMap[placed.uuid]?.let { cached ->
            if (cached.isValid) return cached
            entityMap.remove(placed.uuid)
        }

        val anchor = anchorOf(placed) ?: return null
        val world = anchor.world ?: return null
        if (!world.isChunkLoaded(anchor.blockX shr 4, anchor.blockZ shr 4)) return null

        return world.getNearbyEntities(anchor, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
            .filterIsInstance<LivingEntity>()
            .firstOrNull { !it.isDead && ownerOf(it) == placed.uuid }
            ?.also { entityMap[placed.uuid] = it }
    }

    fun reconcile(placed: PlacedSpawner, type: SpawnerType) {
        val existing = find(placed)

        if (placed.liveMobs <= 0) {
            existing?.remove()
            entityMap.remove(placed.uuid)
            return
        }

        if (existing != null) {
            applyName(existing, placed, type)
            realign(existing, placed)
            return
        }

        val anchor = anchorOf(placed) ?: return
        val world = anchor.world ?: return

        val entityClass = type.entityType.entityClass
            ?.takeIf { LivingEntity::class.java.isAssignableFrom(it) }
            ?.asSubclass(LivingEntity::class.java)
            ?: run {
                Instance.logger.warning(
                    "[spawners] ${type.name} aponta para um EntityType que não é LivingEntity."
                )
                return
            }

        world.spawn(anchor, entityClass) { entity ->
            entityMap[placed.uuid] = entity
            entity.setAI(false)
            entity.isSilent = true
            entity.setGravity(false)
            entity.isPersistent = false
            entity.canPickupItems = false
            entity.persistentDataContainer.set(
                SpawnersSystem.OWNER_SPAWNER,
                PersistentDataType.STRING,
                placed.uuid.toString(),
            )
            applyName(entity, placed, type)
        }
    }

    fun remove(placed: PlacedSpawner) {
        find(placed)?.remove()
        entityMap.remove(placed.uuid)
    }

    private fun realign(entity: LivingEntity, placed: PlacedSpawner) {
        val anchor = anchorOf(placed) ?: return
        if (entity.location.distanceSquared(anchor) < 0.01) return
        entity.teleport(anchor)
    }

    fun refreshName(entity: LivingEntity, placed: PlacedSpawner, type: SpawnerType) =
        applyName(entity, placed, type)

    private fun applyName(entity: LivingEntity, placed: PlacedSpawner, type: SpawnerType) {
        entity.customName(Mini.deserialize("<yellow>x${placed.liveMobs}"))
        entity.isCustomNameVisible = true
    }
}
