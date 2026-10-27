package ocean_block.system.machines.generator

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.libs.instance.Instance
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.custom_items.block.context.PlaceContext
import ocean_block.system.machines.MachineFacing
import org.bukkit.Bukkit
import ocean_block.system.machines.MachineBlock
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.energy.EnergyRules
import org.bukkit.Particle
import org.bukkit.Sound

object GeneratorBlock : MachineBlock() {

    override val id: String = MachineIds.GENERATOR
    override val displayName: String = "<white>Gerador de energia"
    override val lore: List<String> = listOf(
        "<gray>Queima <white>alga seca<gray> e <white>prismarinho<gray>.",
        "<gray>Gera <yellow>40 E/s<gray> enquanto queima.",
        "<gray>Guarda até <yellow>10.000 E<gray>.",
        "<gray>Alimenta máquinas a até <white>8 blocos<gray>.",
    )
    override val limitPerIsland: Int = 4

    override fun onRightClick(ctx: BlockClickContext) {
        val hand = ctx.player.inventory.itemInMainHand
        if (MachineItems.isVanilla(hand) && Fuel.ticks(hand.type.name) != null) {
            val taken = insertFuel(ctx.placed, hand.type.name, hand.amount)
            if (taken > 0) {
                MachineItems.takeFromHand(ctx.player, taken)
                ctx.player.playSound(ctx.player.location, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.8f, 1.2f)
                return
            }
        }
        if (!GeneratorDialog.open(ctx.player, ctx.placed)) ctx.viewFrame.open(GeneratorMenu::class.java, ctx.player, ctx.placed)
    }

    override fun appearanceId(placed: PlacedBlock): String =
        MachineFacing.fromCounter(placed.counters[MachineFacing.KEY]).textureId(id)

    override fun onPlace(ctx: PlaceContext) {
        ctx.placed.counters[MachineFacing.KEY] = MachineFacing.frontFor(ctx.player.facing.name).ordinal.toLong()
        Bukkit.getScheduler().runTask(Instance, Runnable { restoreAppearance(ctx.placed) })
    }

    override fun work(placed: PlacedBlock) {
        val state = GeneratorState.fromCounters(placed.counters)
        val next = GeneratorRules.tick(state, TICKS, Fuel::ticks)
        if (next == state) return
        save(placed, next)
        val location = placed.location.toBukkit() ?: return
        location.world?.spawnParticle(Particle.SMOKE, location.add(0.5, 1.05, 0.5), 1, 0.15, 0.0, 0.15, 0.0)
    }

    fun insertFuel(placed: PlacedBlock, material: String, amount: Int): Int {
        val (next, taken) = GeneratorRules.insertFuel(GeneratorState.fromCounters(placed.counters), material, amount, Fuel::ticks)
        if (taken > 0) save(placed, next)
        return taken
    }

    fun save(placed: PlacedBlock, state: GeneratorState) {
        placed.counters.keys.removeAll { it.startsWith(GeneratorState.PREFIX) || it == EnergyRules.KEY }
        placed.counters.putAll(state.toCounters())
        PlacedBlockManager.markDirty(placed)
    }
}
