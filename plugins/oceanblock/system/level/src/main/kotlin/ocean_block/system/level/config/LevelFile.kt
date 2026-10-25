package ocean_block.system.level.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object LevelFile {

    private const val FILE = "nivel.yml"

    fun load(plugin: JavaPlugin): LevelConfig {
        val file = File(plugin.dataFolder, FILE)
        if (!file.exists()) plugin.saveResource(FILE, false)
        return LevelConfigParser.parse(YamlConfiguration.loadConfiguration(file), plugin.logger::warning)
    }
}
