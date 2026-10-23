package ocean_block.system.stations.smelter

import ocean_block.system.custom_items.block.CustomBlock
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.custom_items.block.context.BreakContext
import org.bukkit.Material
import kotlin.random.Random

class SmelterBlock(val tier: SmelterTier) : CustomBlock() {

    override val id: String = tier.blockId
    override val material: Material = Material.BLAST_FURNACE
    override val displayName: String = "<white>${NAMES.getValue(tier)}"
    override val lore: List<String> = listOfNotNull(
        "<gray>Clique com minério bruto e combustível.",
        "<gray>Cada minério bruto vira 1 lingote.",
        "<gray>Mão vazia retira os lingotes.",
        if (tier.bonusChance > 0) "<gray>Mais rápida, com chance de lingote extra." else null,
        if (tier.hoppers) "<gray>Funciona com funis e baús." else null,
    )
    override val limitPerIsland: Int = 4

    override fun tickInterval(placed: PlacedBlock): Long = TICKS.toLong()

    override fun onRightClick(ctx: BlockClickContext) {
        SmelterActions.use(ctx.player, ctx.block, ctx.placed, takeAll = false)
    }

    override fun onBreak(ctx: BreakContext) {
        SmelterDisplay.remove(ctx.placed.uuid)
    }

    override fun tick(placed: PlacedBlock) {
        val block = placed.location.toBukkit()?.block ?: return
        var state = SmelterState.fromCounters(placed.counters)
        if (tier.hoppers) state = SmelterHoppers.exchange(block, state)
        val next = SmelterRules.tick(state, tier, TICKS, Random.Default)
        if (next != state || next.burnTicks > 0) SmelterActions.save(block, placed, next)
        else SmelterDisplay.update(placed.uuid, block, next.input)
    }

    companion object {
        private const val TICKS = 5
        private val NAMES = mapOf(
            SmelterTier.BASIC to "Fundição",
            SmelterTier.IMPROVED to "Fundição rápida",
            SmelterTier.HOPPER to "Fundição com funil",
        )

        val all: List<SmelterBlock> = SmelterTier.entries.map(::SmelterBlock)

        fun of(id: String): SmelterBlock? = all.firstOrNull { it.id == id }
    }
}
