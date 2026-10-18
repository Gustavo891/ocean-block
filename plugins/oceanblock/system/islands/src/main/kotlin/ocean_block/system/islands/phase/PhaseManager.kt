package ocean_block.system.islands.phase

import ocean_block.libs.instance.Instance
import ocean_block.system.islands.monster.AttributeConfig
import ocean_block.system.islands.monster.DropConfig
import ocean_block.system.islands.monster.DropType
import ocean_block.system.islands.monster.MonsterConfig
import ocean_block.libs.rewards.Reward
import ocean_block.libs.rewards.RewardType
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import java.io.File

object PhaseManager {

    private val phases = mutableMapOf<String, Phase>()
    private val orderedPhases = mutableListOf<Phase>()

    fun phases() = orderedPhases

    fun loadPhases() {
        val folder = File(Instance.dataFolder, "phases")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        phases.clear()
        orderedPhases.clear()

        folder.listFiles { file ->
            file.isFile && file.name.endsWith(".yml")
        }?.forEach { file ->

            val config = YamlConfiguration.loadConfiguration(file)

            val id = config.getString("id") ?: return@forEach
            val prettyName = config.getString("pretty-name") ?: id
            val priority = config.getInt("priority")
            val rawIconMaterial = config.getString("icon-material") ?: "stone"
            val iconMaterial = Material.matchMaterial(rawIconMaterial) ?: Material.STONE

            val blocksToNext = config.getInt("settings.blocks-to-next-phase")

            val blockPool = loadBlockPool(id, config)
            val mobPool = loadMobPool(config)
            val announce = config.getStringList("completion.announce")
            val rewards = loadRewards(config)
            val monsterParty = loadMonsterParty(config)
            val chestLoot = loadChestLoot(id, config)

            val phase = Phase(
                id,
                prettyName,
                priority,
                iconMaterial,
                blocksToNext,
                blockPool,
                mobPool,
                announce,
                rewards,
                monsterParty,
                chestLoot
            )

            phases[id] = phase
            orderedPhases.add(phase)
        }

        orderedPhases.sortBy { it.priority }

        Instance.logger.info("[phase] Loaded ${orderedPhases.map { it.id }} phases.")
    }

    fun get(id: String): Phase? = phases[id]

    fun getNextPhase(currentPriority: Int): Phase? {
        return orderedPhases.firstOrNull { it.priority == currentPriority + 1 }
    }

    private fun loadBlockPool(id: String, config: FileConfiguration): Map<Material, Int> {
        val section = config.getConfigurationSection("block-pool") ?: return emptyMap()

        return section.getKeys(false).mapNotNull { key ->
            val material = Material.matchMaterial(key)

            if (material == null) {
                Instance.logger.warning("[phase] [$id] Material inválido: $key")
                return@mapNotNull null
            }

            if (!material.isBlock) {
                Instance.logger.warning("[phase] [$id] Material não é um bloco: $key")
                return@mapNotNull null
            }

            material to section.getInt(key)
        }.toMap()
    }

    private fun loadMobPool(config: FileConfiguration): Map<EntityType, MobSpawnData> {
        val section = config.getConfigurationSection("mob-pool") ?: return emptyMap()

        return section.getKeys(false).associate { key ->
            val entity = EntityType.valueOf(key.uppercase())

            val weight = config.getInt("mob-pool.$key.weight")
            val min = config.getInt("mob-pool.$key.min")
            val max = config.getInt("mob-pool.$key.max")

            entity to MobSpawnData(weight, min, max)
        }
    }

    private fun loadChestLoot(id: String, config: FileConfiguration): ChestLootTable? {
        val section = config.getConfigurationSection("chest-loot") ?: return null

        val minRolls = section.getInt("rolls-min", 1)
        val maxRolls = section.getInt("rolls-max", minRolls)

        val entries = section.getMapList("items").mapNotNull { map ->
            val rawMaterial = map["material"]?.toString()
            val texturaId = map["textura"]?.toString()

            if (rawMaterial == null && texturaId == null) {
                Instance.logger.warning("[phase] [$id] chest-loot sem 'material' nem 'textura'")
                return@mapNotNull null
            }

            if (rawMaterial != null && texturaId != null) {
                Instance.logger.warning("[phase] [$id] chest-loot com 'material' e 'textura' juntos: $rawMaterial")
                return@mapNotNull null
            }

            val material = rawMaterial?.let { Material.matchMaterial(it) }
            if (rawMaterial != null && material == null) {
                Instance.logger.warning("[phase] [$id] chest-loot com material invalido: $rawMaterial")
                return@mapNotNull null
            }

            val min = (map["min"] as? Number)?.toInt() ?: 1
            val max = (map["max"] as? Number)?.toInt() ?: min

            ChestLootEntry(
                material = material,
                texturaId = texturaId,
                weight = (map["weight"] as? Number)?.toInt() ?: 1,
                min = min,
                max = max
            )
        }

        if (entries.isEmpty()) return null

        return ChestLootTable(minRolls, maxRolls, entries)
    }

    private fun loadRewards(config: FileConfiguration): List<Reward> {
        val list = config.getMapList("completion.rewards")

        return list.mapNotNull { map ->
            val type = map["type"]?.toString()?.uppercase() ?: return@mapNotNull null

            Reward(
                type = EnumUtils.getEnumIgnoreCase(RewardType::class.java, type),
                value = map["value"]?.toString(),
                material = map["material"]?.toString()?.let { Material.matchMaterial(it) },
                amount = map["amount"]?.toString()?.toIntOrNull()
            )
        }
    }

    private fun loadMonsterParty(config: FileConfiguration): List<MonsterConfig>? {
        val maps = config.getMapList("monster-party")
        if (maps.isEmpty()) return null

        return maps.mapNotNull { map ->

            val id = map["id"]?.toString() ?: return@mapNotNull null

            val typeRaw = map["type"]?.toString()?.uppercase() ?: return@mapNotNull null
            val entityType = try {
                EntityType.valueOf(typeRaw)
            } catch (ex: Exception) {
                Instance.logger.warning("Invalid EntityType '$typeRaw' in monster-party ($id)")
                return@mapNotNull null
            }

            val weight = (map["weight"] as? Number)?.toInt() ?: 1
            val min = (map["min"] as? Number)?.toInt() ?: 1
            val max = (map["max"] as? Number)?.toInt() ?: 1
            val name = map["name"]?.toString()
            val isGlowing = (map["is-glowing"] as? Boolean) ?: false

            val attributesMap = map["attributes"] as? Map<*, *>
            val attributes = attributesMap?.let {
                AttributeConfig(
                    (it["max-health"] as? Number)?.toDouble(),
                    (it["attack-damage"] as? Number)?.toDouble(),
                    (it["movement-speed"] as? Number)?.toDouble(),
                    (it["knockback-resistance"] as? Number)?.toDouble(),
                    (it["follow-range"] as? Number)?.toDouble(),
                    (it["scale"] as? Number)?.toDouble()
                )
            }

            val drops = (map["drops"] as? List<Map<String, Any>>)?.mapNotNull { dropMap ->

                val typeRawDrop = dropMap["type"]?.toString()?.uppercase() ?: return@mapNotNull null
                val type = try {
                    DropType.valueOf(typeRawDrop)
                } catch (ex: Exception) {
                    Instance.logger.warning("Invalid drop type '$typeRawDrop' in monster-party ($id)")
                    return@mapNotNull null
                }

                val chance = (dropMap["chance"] as? Number)?.toDouble() ?: 100.0

                when (type) {
                    DropType.ITEM -> {
                        val materialName = dropMap["material"]?.toString() ?: return@mapNotNull null
                        val material = Material.matchMaterial(materialName)

                        if (material == null) {
                            Instance.logger.warning("Invalid material '$materialName' in monster-party ($id)")
                            return@mapNotNull null
                        }

                        val amount = (dropMap["amount"] as? Number)?.toInt() ?: 1

                        DropConfig(
                            type = type,
                            material = material,
                            amount = amount,
                            command = null,
                            chance = chance
                        )
                    }

                    DropType.COMMAND -> {
                        val command = dropMap["command"]?.toString() ?: return@mapNotNull null

                        DropConfig(
                            type = type,
                            material = null,
                            amount = null,
                            command = command,
                            chance = chance
                        )
                    }
                }
            } ?: emptyList()

            MonsterConfig(
                id = id,
                entityType = entityType,
                weight = weight,
                min = min,
                max = max,
                name = name,
                isGlowing = isGlowing,
                attributes = attributes,
                armor = null,
                weaponConfig = null,
                drops = drops
            )
        }
    }
}
