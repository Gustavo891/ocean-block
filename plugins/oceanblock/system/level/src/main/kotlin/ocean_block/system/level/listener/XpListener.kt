package ocean_block.system.level.listener

import ocean_block.system.craft.events.CustomCraftEvent
import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.drift.collect.DebrisCollectedEvent
import ocean_block.system.islands.generator.GeneratorBlockBrokenEvent
import ocean_block.system.level.PlayerLevels
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class XpListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDebris(event: DebrisCollectedEvent) {
        PlayerLevels.award(event.player, PlayerLevels.config.debrisXp(event.typeId))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGenerator(event: GeneratorBlockBrokenEvent) {
        PlayerLevels.award(event.player, PlayerLevels.config.generatorXp(event.material.name))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onCraft(event: CustomCraftEvent) {
        val recipe = Recipes.get(event.id) ?: return
        PlayerLevels.award(event.player, PlayerLevels.config.recipeXp(recipe.level))
    }
}
