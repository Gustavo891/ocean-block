package ocean_block.system.skills

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.skills.command.SkillCommand
import ocean_block.system.skills.craft.SkillRecipeGates
import ocean_block.system.skills.expansion.SkillExpansion
import ocean_block.system.skills.feedback.XpFeedback
import ocean_block.system.skills.listener.PlayerListener
import ocean_block.system.skills.listener.SkillListener
import ocean_block.system.skills.perk.CombatPerks
import ocean_block.system.skills.perk.FarmingPerks
import ocean_block.system.skills.perk.FishingPerks
import ocean_block.system.skills.perk.MiningPerks
import ocean_block.system.skills.skill.Skill
import ocean_block.system.skills.skill.SkillManager
import ocean_block.system.skills.view.SkillTreeView
import ocean_block.system.skills.view.SkillsView
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object SkillsSystem {

    fun init(plugin: JavaPlugin, viewFrame: ViewFrame, commandManager: PaperCommandManager) {
        SkillManager.onInit()

        viewFrame.with(SkillsView(), SkillTreeView())

        commandManager.commandCompletions.registerCompletion("skills") {
            Skill.entries.map { it.id }
        }
        commandManager.registerCommand(SkillCommand(viewFrame))

        SkillRecipeGates.registerAll()
        XpFeedback.start()

        setupListeners(plugin)
        setupExpansions()
    }

    fun onDisable() = SkillManager.onDisable()

    private fun setupListeners(plugin: JavaPlugin) = with(plugin.server.pluginManager) {
        registerEvents(PlayerListener(), plugin)
        registerEvents(SkillListener(), plugin)

        registerEvents(MiningPerks(), plugin)
        registerEvents(FarmingPerks(), plugin)
        registerEvents(CombatPerks(), plugin)
        registerEvents(FishingPerks(), plugin)
    }

    private fun setupExpansions() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return
        SkillExpansion().register()
    }
}
