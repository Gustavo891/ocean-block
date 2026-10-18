package ocean_block.system.drift

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.instance.Instance
import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.drift.config.DriftConfig
import ocean_block.system.drift.config.DriftFile
import ocean_block.system.drift.debris.DebrisManager
import ocean_block.system.drift.hook.FirstJoinListener
import ocean_block.system.drift.hook.GanchoItem
import ocean_block.system.drift.hook.GanchoRecipe
import ocean_block.system.drift.loot.ConfigDebrisPool
import ocean_block.system.drift.net.NetBlock
import ocean_block.system.drift.net.NetListener
import ocean_block.system.drift.net.NetRecipe
import ocean_block.system.drift.net.view.NetStorageView
import org.bukkit.plugin.java.JavaPlugin

object DriftSystem {

    var config: DriftConfig? = null
        private set

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        CustomItemRegistry.register(GanchoItem)
        GanchoRecipe.register()
        registerNet(plugin, viewFrame)
        plugin.server.pluginManager.registerEvents(FirstJoinListener(), plugin)

        config = runCatching { DriftFile.load(plugin) }
            .onFailure { Instance.logger.severe("[deriva] falha ao carregar drift.yml: ${it.message}") }
            .getOrNull()

        config?.let { DebrisManager.start(it, ConfigDebrisPool(it.types)) }
    }

    fun onDisable() {
        DebrisManager.stop()
    }

    private fun registerNet(plugin: JavaPlugin, viewFrame: ViewFrame) {
        CustomBlockRegistry.register(NetBlock)
        NetRecipe.register()
        viewFrame.with(NetStorageView())
        plugin.server.pluginManager.registerEvents(NetListener(viewFrame), plugin)
    }
}
