package ocean_block.system.skills.perk

import ocean_block.system.skills.Skills
import ocean_block.system.skills.skill.SkillPerk
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import java.util.concurrent.ThreadLocalRandom

class FishingPerks : Listener {

    private companion object {

        const val VANILLA_MIN_TICKS = 100
        const val VANILLA_MAX_TICKS = 600

        const val FAST_BAIT_MIN_TICKS = 40
        const val FAST_BAIT_MAX_TICKS = 200

        const val NET_CHANCE = 0.35
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        when (event.state) {
            PlayerFishEvent.State.FISHING -> applyFastBait(event)
            PlayerFishEvent.State.CAUGHT_FISH -> applyNet(event)
            else -> Unit
        }
    }

    private fun applyFastBait(event: PlayerFishEvent) {
        val power = Skills.perkPower(event.player, SkillPerk.FastBait)
        if (power <= 0.0) return

        event.hook.minWaitTime = interpolate(VANILLA_MIN_TICKS, FAST_BAIT_MIN_TICKS, power)
        event.hook.maxWaitTime = interpolate(VANILLA_MAX_TICKS, FAST_BAIT_MAX_TICKS, power)
    }

    private fun interpolate(from: Int, to: Int, power: Double): Int =
        (from - (from - to) * power).toInt()

    private fun applyNet(event: PlayerFishEvent) {
        if (!chance(Skills.perkPower(event.player, SkillPerk.Net) * NET_CHANCE)) return

        val caught = event.caught as? Item ?: return
        val stack = caught.itemStack

        stack.amount *= 2
        caught.itemStack = stack
    }

    private fun chance(probability: Double): Boolean =
        ThreadLocalRandom.current().nextDouble() < probability
}
