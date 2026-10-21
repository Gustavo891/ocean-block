package ocean_block.system.stations.config

import ocean_block.libs.instance.Instance
import ocean_block.system.stations.item.OrePiece
import ocean_block.system.stations.sieve.SieveTable
import ocean_block.system.stations.sieve.SieveTableParser
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object StationsFile {

    private const val SIEVE_FILE = "peneira.yml"

    fun loadSieve(plugin: JavaPlugin): SieveTable {
        val file = File(plugin.dataFolder, SIEVE_FILE)
        if (!file.exists()) plugin.saveResource(SIEVE_FILE, false)
        return SieveTableParser.parse(
            YamlConfiguration.loadConfiguration(file),
            { output -> OrePiece.byId(output) != null || Material.matchMaterial(output)?.isItem == true },
            Instance.logger::warning,
        )
    }
}
