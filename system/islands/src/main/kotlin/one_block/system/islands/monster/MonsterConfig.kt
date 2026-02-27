package one_block.system.islands.monster

import org.bukkit.entity.EntityType

data class MonsterConfig(
    val id: String,
    val entityType: EntityType,
    val weight: Int,
    val min: Int,
    val max: Int,
    val name: String?,
    val isGlowing: Boolean?,
    val attributes: AttributeConfig?,
    val armor: ArmorConfig?,
    val weaponConfig: WeaponConfig?,
    val drops: List<DropConfig>,
)