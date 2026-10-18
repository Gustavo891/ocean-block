package ocean_block.system.spawn.spawn

import ocean_block.libs.instance.Instance
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

internal object SpawnFile {

    private lateinit var file: File
    private lateinit var config: FileConfiguration

    fun setup() {
        file = File(Instance.dataFolder, "spawn.yml")
        if (!file.exists()) {
            Instance.dataFolder.mkdirs()
            file.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(file)
    }

    fun config(): FileConfiguration = config

    fun save() = config.save(file)
}
