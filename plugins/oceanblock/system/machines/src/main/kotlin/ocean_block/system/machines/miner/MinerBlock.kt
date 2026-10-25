package ocean_block.system.machines.miner

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.custom_items.block.context.BreakContext
import ocean_block.system.custom_items.block.context.PlaceContext
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.machines.MachineBlock
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.energy.EnergyNetwork
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object MinerBlock : MachineBlock() {

    const val FULL_KEY = "minerador_cheio"

    private val OUTPUT_FACES = listOf(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
    private val TOOL = ItemStack(Material.IRON_PICKAXE)

    override val id: String = MachineIds.MINER
    override val displayName: String = "<white>Minerador"
    override val lore: List<String> = listOf(
        "<gray>Quebra a pedra ou o minério logo abaixo,",
        "<gray>gastando <yellow>40 E <gray>por bloco.",
        "<gray>Vem com broca de ferro (<white>2 s<gray> por bloco);",
        "<gray>brocas melhores se encaixam no menu.",
        "<gray>Guarda 27 pilhas e entrega em baú encostado.",
        "<gray>Precisa de energia a até <white>8 blocos<gray>.",
    )
    override val limitPerIsland: Int = 4
    override val material: Material = Material.BARRIER
    override val texturaBlock: Boolean = false

    override fun storageCapacity(placed: PlacedBlock): Int = MinerRules.STORAGE

    override fun onRightClick(ctx: BlockClickContext) {
        ctx.viewFrame.open(MinerMenu::class.java, ctx.player, ctx.placed)
    }

    override fun onPlace(ctx: PlaceContext) {
        MinerDisplay.ensure(ctx.placed, ctx.block)
    }

    override fun onBreak(ctx: BreakContext) {
        MinerDisplay.remove(ctx.placed.uuid)
        MinerCracks.clear(ctx.placed.uuid)
    }

    override fun adopt(placed: PlacedBlock, block: Block): Boolean {
        if (!MinerRules.adopts(block.type.name)) return false
        block.setType(Material.BARRIER, false)
        return true
    }

    override fun work(placed: PlacedBlock) {
        val block = placed.location.toBukkit()?.block ?: return
        MinerDisplay.ensure(placed, block)
        pushStorage(placed, block)
        if (!isOn(placed)) {
            MinerCracks.clear(placed.uuid)
            return EnergyNetwork.setNoPower(placed, false)
        }
        val tier = tier(placed)
        if (!EnergyNetwork.draw(placed, tier.cycleEnergy)) return MinerCracks.clear(placed.uuid)
        MinerDisplay.spin(placed, TICKS)
        val progress = MinerRules.advance(placed.counters[MinerRules.PROGRESS_KEY]?.toInt() ?: 0, tier.gain)
        setCounter(placed, MinerRules.PROGRESS_KEY, progress.toLong())
        val below = block.getRelative(BlockFace.DOWN)
        if (!canBreak(placed, block, below)) return MinerCracks.clear(placed.uuid)
        if (!MinerRules.isDue(progress)) return MinerCracks.update(placed.uuid, below, progress, tier.gain)
        val drops = below.getDrops(TOOL).toList()
        val storage = placed.buildStorageInventory(MinerRules.STORAGE)
        val full = !fits(storage, drops)
        setCounter(placed, FULL_KEY, if (full) 1 else 0)
        if (full) return MinerCracks.clear(placed.uuid)

        MinerCracks.clear(placed.uuid)
        breakBlock(below)
        val leftovers = drops.flatMap { drop -> outputContainer(block)?.addItem(drop)?.values ?: listOf(drop) }
        leftovers.forEach { storage.addItem(it) }
        placed.setStorage(storage)
        setCounter(placed, MinerRules.PROGRESS_KEY, MinerRules.afterBreak(progress).toLong())
        PlacedBlockManager.markDirty(placed)
    }

    fun tier(placed: PlacedBlock): DrillTier = DrillTier.of(placed.counters[DrillTier.KEY])

    fun setTier(placed: PlacedBlock, tier: DrillTier) {
        if (tier == DrillTier.IRON) placed.counters.remove(DrillTier.KEY) else placed.counters[DrillTier.KEY] = tier.level.toLong()
        PlacedBlockManager.saveNow(placed)
        placed.location.toBukkit()?.block?.let { MinerDisplay.ensure(placed, it) }
    }

    fun isOn(placed: PlacedBlock): Boolean = MinerRules.isOn(placed.counters[MinerRules.ON_KEY])

    fun toggle(placed: PlacedBlock): Boolean {
        val on = !isOn(placed)
        placed.counters[MinerRules.ON_KEY] = if (on) 1 else 0
        if (!on) EnergyNetwork.setNoPower(placed, false)
        PlacedBlockManager.saveNow(placed)
        return on
    }

    fun belowLabel(placed: PlacedBlock): String {
        val block = placed.location.toBukkit()?.block ?: return "?"
        val below = block.getRelative(BlockFace.DOWN)
        return if (canBreak(placed, block, below)) "<green>pronto para minerar" else "<red>nada para minerar"
    }

    private fun canBreak(placed: PlacedBlock, block: Block, below: Block): Boolean {
        if (!MinerRules.canMine(below.type.name)) return false
        if (PlacedBlockManager.get(below) != null) return false
        val island = IslandManager.getIslandAt(block.location) ?: return false
        return IslandManager.getIslandAt(below.location)?.uuid == island.uuid
    }

    private fun fits(storage: Inventory, drops: List<ItemStack>): Boolean {
        val probe = Bukkit.createInventory(null, storage.size)
        probe.contents = storage.contents.map { it?.clone() }.toTypedArray()
        return drops.all { probe.addItem(it.clone()).isEmpty() }
    }

    private fun breakBlock(below: Block) {
        val center = below.location.add(0.5, 0.5, 0.5)
        below.world.spawnParticle(Particle.BLOCK, center, 20, 0.3, 0.3, 0.3, below.blockData)
        below.world.playSound(center, below.blockData.soundGroup.breakSound, 0.6f, 1f)
        below.type = Material.AIR
    }

    private fun pushStorage(placed: PlacedBlock, block: Block) {
        val items = placed.storageItems()
        if (items.isEmpty()) return
        val container = outputContainer(block) ?: return
        val left = container.addItem(*items.toTypedArray()).values.toList()
        if (left.size == items.size && left.zip(items).all { (a, b) -> a.amount == b.amount }) return
        placed.setStorage(left)
        setCounter(placed, FULL_KEY, 0)
        PlacedBlockManager.markDirty(placed)
    }

    private fun outputContainer(block: Block): Inventory? = OUTPUT_FACES.asSequence()
        .map { block.getRelative(it) }
        .filter { PlacedBlockManager.get(it) == null }
        .mapNotNull { (it.getState(false) as? Container)?.inventory }
        .firstOrNull()

    private fun setCounter(placed: PlacedBlock, key: String, value: Long) {
        if ((placed.counters[key] ?: 0L) == value) return
        placed.counters[key] = value
        PlacedBlockManager.markDirty(placed)
    }
}
