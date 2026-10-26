package ocean_block.system.playtime_rewards.config

import ocean_block.system.playtime_rewards.model.Milestone
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object RewardsFile {

    private const val FILE = "recompensas.yml"

    fun load(plugin: JavaPlugin): List<Milestone> {
        val file = File(plugin.dataFolder, FILE)
        if (!file.exists()) plugin.saveResource(FILE, false)
        val ladder = RewardsConfigParser.parse(
            YamlConfiguration.loadConfiguration(file),
            { name -> Material.matchMaterial(name)?.isItem == true },
            plugin.logger::warning,
        )
        if (ladder.isEmpty()) plugin.logger.severe("[recompensas] Nenhum marco válido em $FILE; recompensas por tempo desligadas.")
        return ladder
    }
}
