package ocean_block.system.machines.furnace

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.machines.MachineBlock
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.energy.EnergyNetwork
import org.bukkit.Particle

object FurnaceBlock : MachineBlock() {

    override val id: String = MachineIds.FURNACE
    override val displayName: String = "<white>Fornalha elétrica"
    override val lore: List<String> = listOf(
        "<gray>Derrete <white>4 itens <gray>ao mesmo tempo,",
        "<gray>4 vezes mais rápido que a Fundição.",
        "<gray>Gasta <yellow>20 E/s <gray>por item derretendo.",
        "<gray>Precisa de energia a até <white>8 blocos<gray>.",
    )
    override val limitPerIsland: Int = 4

    override fun onRightClick(ctx: BlockClickContext) {
        ctx.viewFrame.open(FurnaceMenu::class.java, ctx.player, ctx.placed)
    }

    override fun tick(placed: PlacedBlock) {
        val recipes = FurnaceRecipes.results
        val state = FurnaceState.fromCounters(placed.counters)
        val needed = FurnaceRules.energyNeeded(state, TICKS, recipes)
        if (needed == 0L) return EnergyNetwork.setNoPower(placed, false)
        if (!EnergyNetwork.draw(placed, needed)) return
        save(placed, FurnaceRules.tick(state, TICKS, recipes))
        val location = placed.location.toBukkit() ?: return
        location.world?.spawnParticle(Particle.ELECTRIC_SPARK, location.add(0.5, 1.05, 0.5), 2, 0.2, 0.05, 0.2, 0.0)
    }

    fun save(placed: PlacedBlock, state: FurnaceState) {
        placed.counters.keys.removeAll { it.startsWith(FurnaceState.PREFIX) }
        placed.counters.putAll(state.toCounters())
        PlacedBlockManager.markDirty(placed)
    }
}
