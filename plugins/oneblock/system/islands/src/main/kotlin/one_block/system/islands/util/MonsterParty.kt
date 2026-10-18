package one_block.system.islands.util

import one_block.system.islands.monster.AttributeConfig
import one_block.system.islands.monster.MonsterConfig
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity

object MonsterParty {

    fun spawnConfigureMob(location: Location, config: MonsterConfig) {
        val add = location.clone().add(0.5, 2.0, 0.5)
        val entity = add.world.spawnEntity(add, config.entityType) as LivingEntity

        config.name?.let {
            entity.customName = it
            entity.isCustomNameVisible = true
        }

        config.isGlowing?.let { entity.isGlowing = it }
        applyAttributes(entity, config.attributes)
    }

    private fun applyAttributes(entity: LivingEntity, config: AttributeConfig?) {
        config ?: return

        config.maxHealth?.let {
            entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue = it
            entity.health = it
        }

        config.attackDamage?.let {
            entity.getAttribute(Attribute.ATTACK_DAMAGE)?.baseValue = it
        }

        config.movementSpeed?.let {
            entity.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = it
        }

        config.knockbackResistance?.let {
            entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = it
        }

        config.followRange?.let {
            entity.getAttribute(Attribute.FOLLOW_RANGE)?.baseValue = it
        }

        config.scale?.let {
            entity.getAttribute(Attribute.SCALE)?.baseValue = it
        }
    }
}
