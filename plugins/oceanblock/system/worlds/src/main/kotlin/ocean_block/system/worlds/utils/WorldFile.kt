package ocean_block.system.worlds.utils

import ocean_block.libs.instance.Instance
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object WorldFile {

    private lateinit var file: File
    private lateinit var config: FileConfiguration

    fun setup() {
        file = File(Instance.dataFolder, "worlds.yml")
        if (!file.exists()) {
            Instance.dataFolder.mkdirs()
            file.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(file)
    }

    fun config() = config

    fun save() = config.save(file)
}
