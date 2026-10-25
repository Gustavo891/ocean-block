package ocean_block.system.level

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.connection.Mongo
import ocean_block.libs.instance.ScheduledExecutor
import ocean_block.system.craft.recipe.RecipeLocks
import ocean_block.system.level.command.NivelCommand
import ocean_block.system.level.config.LevelFile
import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.level.data.LevelManager
import ocean_block.system.level.listener.XpListener
import ocean_block.system.level.menu.LevelMenu
import ocean_block.system.level.notify.LevelNotifier
import ocean_block.system.scoreboard.level.AccountLevel
import one_block.textura.api.NotificationApi
import org.bson.Document
import org.bukkit.plugin.java.JavaPlugin

object LevelSystem {

    private const val OLD_RESEARCH_COLLECTION = "island_research"

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        PlayerLevels.config = LevelFile.load(plugin)
        LevelManager.onInit()
        RecipeLocks.install { PlayerLevels.levelOf(it) }
        AccountLevel.source = { LevelCurve.progress(LevelManager.xp(it.uniqueId)) }
        NotificationApi.registerItemIcons(listOf(LevelNotifier.ICON_MATERIAL))
        viewFrame.with(LevelMenu())
        plugin.server.pluginManager.registerEvents(XpListener(), plugin)
        plugin.server.pluginManager.registerEvents(LevelNotifier, plugin)
        paperCommandManager.registerCommand(NivelCommand(viewFrame))
        dropOldResearch(plugin)
    }

    fun onDisable() {
        LevelManager.onDisable()
    }

    private fun dropOldResearch(plugin: JavaPlugin) {
        ScheduledExecutor.execute {
            runCatching { Mongo.getCollection<Document>(OLD_RESEARCH_COLLECTION).drop() }
                .onFailure { plugin.logger.warning("[nivel] Não foi possível apagar $OLD_RESEARCH_COLLECTION: ${it.message}") }
        }
    }
}
