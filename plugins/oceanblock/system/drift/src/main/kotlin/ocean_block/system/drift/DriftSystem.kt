package ocean_block.system.drift

import ocean_block.libs.instance.Instance
import ocean_block.system.drift.config.DriftConfig
import ocean_block.system.drift.config.DriftFile
import ocean_block.system.drift.debris.DebrisManager
import ocean_block.system.drift.loot.ConfigDebrisPool
import org.bukkit.plugin.java.JavaPlugin

object DriftSystem {

    var config: DriftConfig? = null
        private set

    fun init(plugin: JavaPlugin) {
        config = runCatching { DriftFile.load(plugin) }
            .onFailure { Instance.logger.severe("[deriva] falha ao carregar drift.yml: ${it.message}") }
            .getOrNull()

        config?.let { DebrisManager.start(it, ConfigDebrisPool(it.types)) }
    }

    fun onDisable() {
        DebrisManager.stop()
    }
}
