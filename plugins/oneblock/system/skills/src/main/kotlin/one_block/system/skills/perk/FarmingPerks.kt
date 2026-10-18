package one_block.system.skills.perk

import one_block.libs.instance.Instance
import one_block.system.skills.Skills
import one_block.system.skills.skill.Skill
import one_block.system.skills.skill.SkillPerk
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import java.util.concurrent.ThreadLocalRandom

class FarmingPerks : Listener {

    private companion object {

        const val AREA_RADIUS = 1

        val SEED_OF = mapOf(
            Material.WHEAT to Material.WHEAT_SEEDS,
            Material.CARROTS to Material.CARROT,
            Material.POTATOES to Material.POTATO,
            Material.BEETROOTS to Material.BEETROOT_SEEDS,
            Material.NETHER_WART to Material.NETHER_WART,
        )
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockDropItem(event: BlockDropItemEvent) {
        if (!chance(Skills.perkPower(event.player, SkillPerk.Replant))) return

        val cropType = event.blockState.type
        if (!isMature(cropType, event.blockState.blockData)) return

        val seed = SEED_OF[cropType] ?: return

        val consumed = event.items.firstOrNull { it.itemStack.type == seed } ?: return
        val stack = consumed.itemStack

        if (stack.amount <= 1) consumed.remove() else {
            stack.amount -= 1
            consumed.itemStack = stack
        }

        replant(event.blockState.block, cropType)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (!chance(Skills.perkPower(player, SkillPerk.AreaHarvest))) return

        val center = event.block
        if (!isMature(center)) return

        harvestArea(player, center)
    }

    private fun harvestArea(player: Player, center: Block) {
        val replantChance = Skills.perkPower(player, SkillPerk.Replant)

        for (dx in -AREA_RADIUS..AREA_RADIUS) {
            for (dz in -AREA_RADIUS..AREA_RADIUS) {
                if (dx == 0 && dz == 0) continue

                val block = center.world.getBlockAt(center.x + dx, center.y, center.z + dz)
                if (!isMature(block)) continue

                val cropType = block.type

                block.breakNaturally(player.inventory.itemInMainHand)
                Skills.addXp(
                    player.uniqueId,
                    Skill.Farming,
                    Skill.Farming.xpPerAction,
                    block.location.toCenterLocation(),
                )

                if (chance(replantChance)) replant(block, cropType)
            }
        }
    }

    private fun replant(block: Block, cropType: Material) {
        Bukkit.getScheduler().runTask(Instance, Runnable {
            if (!block.type.isAir) return@Runnable

            block.type = cropType
            val data = block.blockData as? Ageable ?: return@Runnable
            data.age = 0
            block.blockData = data
        })
    }

    private fun chance(probability: Double): Boolean =
        ThreadLocalRandom.current().nextDouble() < probability

    private fun isMature(block: Block): Boolean = isMature(block.type, block.blockData)

    private fun isMature(material: Material, data: BlockData): Boolean {
        if (!Tag.CROPS.isTagged(material)) return false

        val ageable = data as? Ageable ?: return true
        return ageable.age >= ageable.maximumAge
    }
}
