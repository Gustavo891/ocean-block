package one_block.system.mobdrops.manager

import one_block.libs.instance.Instance
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import java.io.File

object MobDropManager {

    val removedDrops = mutableMapOf<EntityType, List<Material>>()

    fun onInit() {
        load()
    }

    fun load() {
        removedDrops.clear()

        val file = File(Instance.dataFolder, "mob-drops.yml")
        if (!file.exists()) {
            Instance.saveResource("mob-drops.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("remove-drops") ?: return

        for (key in section.getKeys(false)) {
            val type = EntityType.valueOf(key)
            val materials = section.getStringList(key).map { Material.valueOf(it) }
            removedDrops[type] = materials
        }
    }
}
