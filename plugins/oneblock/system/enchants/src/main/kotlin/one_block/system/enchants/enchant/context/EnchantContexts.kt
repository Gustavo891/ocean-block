package one_block.system.enchants.enchant.context

import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack

data class BreakContext(
    val player: Player,
    val block: Block,
    val item: ItemStack,
    val level: Int,
    val event: BlockBreakEvent,
)

data class AttackContext(
    val attacker: Player,
    val victim: LivingEntity,
    val item: ItemStack,
    val level: Int,
    val event: EntityDamageByEntityEvent,
)

data class DamagedContext(
    val victim: Player,
    val attacker: Entity?,
    val item: ItemStack,
    val level: Int,
    val event: EntityDamageByEntityEvent,
)
