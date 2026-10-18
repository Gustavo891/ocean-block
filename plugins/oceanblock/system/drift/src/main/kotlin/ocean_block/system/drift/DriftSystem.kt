package ocean_block.system.drift

import ocean_block.system.drift.config.DriftConfig
import ocean_block.system.drift.config.DriftFile
import org.bukkit.plugin.java.JavaPlugin

object DriftSystem {

    lateinit var config: DriftConfig
        private set

    var enabled = false
        private set

    fun init(plugin: JavaPlugin) {
        val loaded = DriftFile.load(plugin) ?: return
        config = loaded
        enabled = true
    }

    fun onDisable() {
    }
}
