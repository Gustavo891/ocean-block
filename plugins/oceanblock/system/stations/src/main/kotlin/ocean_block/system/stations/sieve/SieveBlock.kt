package ocean_block.system.stations.sieve

import ocean_block.system.custom_items.block.CustomBlock
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.custom_items.block.context.BreakContext
import org.bukkit.Material

class SieveBlock(val tier: SieveTier) : CustomBlock() {

    override val id: String = tier.blockId
    override val material: Material = Material.COMPOSTER
    override val displayName: String = "<white>${NAMES.getValue(tier)}"
    override val lore: List<String> = when (tier) {
        SieveTier.AUTOMATIC -> listOf(
            "<gray>Puxa areia, cascalho ou terra",
            "<gray>do baú acima e solta o resultado",
            "<gray>no baú ou funil abaixo.",
        )
        else -> listOf(
            "<gray>Clique com areia, cascalho ou terra",
            "<gray>e depois clique até peneirar.",
            "<gray>Etapas: ${tier.steps}",
        )
    }
    override val limitPerIsland: Int = 6

    override fun tickInterval(placed: PlacedBlock): Long = if (tier == SieveTier.AUTOMATIC) AUTO_TICKS else IDLE_TICKS

    override fun onRightClick(ctx: BlockClickContext) {
        SieveActions.use(ctx.player, ctx.block, ctx.placed, tier, quick = false)
    }

    override fun onBreak(ctx: BreakContext) {
        ctx.placed.counters.putAll(SieveProgress.toCounters(SieveProgress.EMPTY))
    }

    override fun tick(placed: PlacedBlock) {
        if (tier == SieveTier.AUTOMATIC) AutoSieve.tick(placed, tier)
    }

    companion object {
        private const val AUTO_TICKS = 20L
        private const val IDLE_TICKS = 1200L
        private val NAMES = mapOf(
            SieveTier.WOOD to "Peneira",
            SieveTier.IRON to "Peneira de ferro",
            SieveTier.DIAMOND to "Peneira de diamante",
            SieveTier.AUTOMATIC to "Peneira automática",
        )

        val all: List<SieveBlock> = SieveTier.entries.map(::SieveBlock)

        fun of(id: String): SieveBlock? = all.firstOrNull { it.id == id }
    }
}
