package ocean_block.system.pirate_raid.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object RaidFile {

    private const val FILE = "piratas.yml"

    fun load(plugin: JavaPlugin): RaidConfig {
        val file = File(plugin.dataFolder, FILE)
        if (!file.exists()) plugin.saveResource(FILE, false)
        return RaidConfigParser.parse(
            YamlConfiguration.loadConfiguration(file),
            { name -> Material.matchMaterial(name)?.isItem == true },
            plugin.logger::warning,
        )
    }
}
