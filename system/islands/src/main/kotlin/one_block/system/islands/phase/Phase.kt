package one_block.system.islands.phase

import one_block.system.islands.monster.MonsterConfig
import one_block.system.islands.reward.Reward
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
    val rewards: List<Reward>,
    val monsterParty: List<MonsterConfig>?,
)