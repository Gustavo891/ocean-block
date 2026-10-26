package ocean_block.system.drift

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.instance.Instance
import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.drift.collect.CollectNotifier
import ocean_block.system.drift.config.DriftConfig
import ocean_block.system.drift.config.DriftFile
import ocean_block.system.drift.debris.DebrisManager
import ocean_block.system.drift.hook.FirstJoinListener
import ocean_block.system.drift.hook.GanchoItem
import ocean_block.system.drift.loot.ConfigDebrisPool
import ocean_block.system.drift.net.NetBlock
import ocean_block.system.drift.net.NetDisplay
import ocean_block.system.drift.net.NetFullNotifier
import ocean_block.system.drift.net.NetListener
import ocean_block.system.drift.net.view.NetStorageView
import ocean_block.system.drift.recipe.DriftRecipes
import one_block.textura.api.NotificationApi
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

object DriftSystem {

    private const val DISPLAY_CLEANUP_TICKS = 100L
    private const val SAMPLE_AMOUNT = 3

    var config: DriftConfig? = null
        private set

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame) {
        CustomItemRegistry.register(GanchoItem)
        registerNet(plugin, viewFrame)
        DriftRecipes.register()
        plugin.server.pluginManager.registerEvents(FirstJoinListener(), plugin)

        config = runCatching { DriftFile.load(plugin) }
            .onFailure { Instance.logger.severe("[deriva] falha ao carregar drift.yml: ${it.message}") }
            .getOrNull()

        config?.let { DebrisManager.start(it, ConfigDebrisPool(it.types)) }
        registerNotifications(plugin)
    }

    fun onDisable() {
        DebrisManager.stop()
        NetDisplay.removeAll()
    }

    private fun registerNotifications(plugin: JavaPlugin) {
        plugin.server.pluginManager.registerEvents(CollectNotifier, plugin)
        val materials = config?.types.orEmpty().flatMap { type -> type.drops.map { it.material } }.toSet()
        val icons = materials.map { CollectNotifier.iconOf(it).removePrefix("item:") }
        NotificationApi.registerItemIcons(icons.distinct())
        NotificationApi.registerSample("coleta") { CollectNotifier.notify(it, materials.firstOrNull() ?: Material.KELP, SAMPLE_AMOUNT) }
        NotificationApi.registerSample("rede_cheia") { NetFullNotifier.preview(it) }
    }

    private fun registerNet(plugin: JavaPlugin, viewFrame: ViewFrame) {
        CustomBlockRegistry.register(NetBlock)
        viewFrame.with(NetStorageView())
        plugin.server.pluginManager.registerEvents(NetListener(viewFrame), plugin)
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { NetDisplay.cleanup() }, 100L, DISPLAY_CLEANUP_TICKS)
    }
}
