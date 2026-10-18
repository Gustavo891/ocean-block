package ocean_block.system.drift.config

import ocean_block.libs.instance.Instance
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object DriftFile {

    private const val FILE_NAME = "drift.yml"

    fun load(plugin: JavaPlugin): DriftConfig? {
        val file = File(plugin.dataFolder, FILE_NAME)
        if (!file.exists()) plugin.saveResource(FILE_NAME, false)

        val config = DriftConfigParser.parse(YamlConfiguration.loadConfiguration(file), Instance.logger::warning)
        if (config == null) Instance.logger.severe("[deriva] drift.yml sem tipos validos; deriva desligada.")
        return config
    }
}
