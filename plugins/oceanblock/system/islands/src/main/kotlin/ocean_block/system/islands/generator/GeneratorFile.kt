package ocean_block.system.islands.generator

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object GeneratorFile {

    private const val FILE = "gerador.yml"

    fun load(plugin: JavaPlugin): GeneratorTable {
        val file = File(plugin.dataFolder, FILE)
        if (!file.exists()) plugin.saveResource(FILE, false)
        return GeneratorTableParser.parse(
            YamlConfiguration.loadConfiguration(file),
            { Material.matchMaterial(it)?.isBlock == true },
            plugin.logger::warning,
        )
    }
}
