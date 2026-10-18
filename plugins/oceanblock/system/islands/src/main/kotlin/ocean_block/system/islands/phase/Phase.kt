package ocean_block.system.islands.phase

import ocean_block.system.islands.monster.MonsterConfig
import ocean_block.libs.rewards.Reward
import org.bukkit.Material
import org.bukkit.entity.EntityType

data class Phase(
    val id: String,
    val prettyName: String,
    val priority: Int,
    val iconMaterial: Material,
    val blocksToNextPhase: Int,
    val blockPool: Map<Material, Int>,
    val mobPool: Map<EntityType, MobSpawnData>,
    val announce: List<String>,
    val rewards: List<Reward>,
    val monsterParty: List<MonsterConfig>?,
    val chestLoot: ChestLootTable? = null,
)
