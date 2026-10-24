package ocean_block.system.machines.furnace

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.machines.MachineBlock
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.energy.EnergyNetwork
import ocean_block.system.machines.MachineItems
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Lightable

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
    override val material: Material = Material.BLAST_FURNACE
    override val texturaBlock: Boolean = false
    override val itemModel: NamespacedKey? get() = null

    override fun onRightClick(ctx: BlockClickContext) {
        val hand = ctx.player.inventory.itemInMainHand
        if (MachineItems.isVanilla(hand)) {
            val taken = insert(ctx.placed, hand.type.name, hand.amount)
            if (taken > 0) {
                MachineItems.takeFromHand(ctx.player, taken)
                ctx.player.playSound(ctx.player.location, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.6f)
                return
            }
        }
        ctx.viewFrame.open(FurnaceMenu::class.java, ctx.player, ctx.placed)
    }

    fun insert(placed: PlacedBlock, material: String, amount: Int): Int {
        val (next, taken) = FurnaceRules.insert(FurnaceState.fromCounters(placed.counters), material, amount, FurnaceRecipes.results)
        if (taken > 0) save(placed, next)
        return taken
    }

    override fun adopt(placed: PlacedBlock, block: Block): Boolean {
        if (!FurnaceRules.adopts(block.type.name)) return false
        block.setType(Material.BLAST_FURNACE, false)
        return true
    }

    override fun work(placed: PlacedBlock) {
        val block = placed.location.toBukkit()?.block ?: return
        val recipes = FurnaceRecipes.results
        val state = FurnaceState.fromCounters(placed.counters)
        val needed = FurnaceRules.energyNeeded(state, TICKS, recipes)
        if (needed == 0L) {
            EnergyNetwork.setNoPower(placed, false)
            return setLit(block, false)
        }
        if (!EnergyNetwork.draw(placed, needed)) return setLit(block, false)
        save(placed, FurnaceRules.tick(state, TICKS, recipes))
        setLit(block, true)
        block.world.spawnParticle(Particle.ELECTRIC_SPARK, block.location.add(0.5, 1.05, 0.5), 2, 0.2, 0.05, 0.2, 0.0)
    }

    private fun setLit(block: Block, lit: Boolean) {
        val data = block.blockData as? Lightable ?: return
        if (data.isLit == lit) return
        data.isLit = lit
        block.setBlockData(data, false)
    }

    fun save(placed: PlacedBlock, state: FurnaceState) {
        placed.counters.keys.removeAll { it.startsWith(FurnaceState.PREFIX) }
        placed.counters.putAll(state.toCounters())
        PlacedBlockManager.markDirty(placed)
    }
}
