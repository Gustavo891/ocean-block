package ocean_block.libs.instance

import org.bukkit.plugin.java.JavaPlugin

val Instance: JavaPlugin get() = PluginInstance

lateinit var PluginInstance: JavaPlugin
