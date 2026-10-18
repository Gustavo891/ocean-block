package one_block.system.spawn.world

import one_block.libs.instance.Instance
import one_block.system.spawn.spawn.SpawnManager
import org.bukkit.GameRule
import org.bukkit.World

object SpawnWorldRules {

    private const val NOON = 6000L

    fun apply() {
        val world = SpawnManager.location()?.toBukkit()?.world ?: return
        apply(world)
    }

    fun apply(world: World) {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        world.setGameRule(GameRule.DO_FIRE_TICK, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.MOB_GRIEFING, false)

        world.time = NOON
        world.setStorm(false)
        world.isThundering = false

        Instance.logger.info("[spawn] regras aplicadas ao mundo '${world.name}'.")
    }
}
