package ocean_block.system.machines.solar

import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.machines.MachineBlock
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.energy.EnergyEffects
import ocean_block.system.machines.energy.EnergyNetwork
import ocean_block.system.machines.energy.EnergyRules
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block

object SolarBlock : MachineBlock() {

    private const val OPEN_SKY = 15

    override val id: String = MachineIds.SOLAR
    override val displayName: String = "<white>Painel solar"
    override val lore: List<String> = listOf(
        "<gray>Gera <yellow>6 E/s <gray>de dia, com céu aberto.",
        "<gray>Guarda até <yellow>1.000 E<gray>.",
        "<gray>Alimenta máquinas a até <white>8 blocos<gray>.",
    )
    override val limitPerIsland: Int = 8
    override val material: Material = Material.DAYLIGHT_DETECTOR
    override val texturaBlock: Boolean = false
    override val itemModel: NamespacedKey? get() = null

    override fun adopt(placed: PlacedBlock, block: Block): Boolean {
        if (!SolarRules.adopts(block.type.name)) return false
        block.setType(Material.DAYLIGHT_DETECTOR, false)
        return true
    }

    override fun onRightClick(ctx: BlockClickContext) {
        val stored = NumberFormatter.format(EnergyNetwork.stored(ctx.placed).toDouble())
        val state = if (isActive(ctx.block)) "<green>gerando 6 E/s" else "<red>parado (noite ou sem céu aberto)"
        ctx.player.sendRichMessage("<yellow>Painel solar: <white>$stored<gray>/1.000 E, $state")
        val showing = EnergyEffects.toggleRadius(ctx.player, ctx.placed)
        ctx.player.sendRichMessage(if (showing) "<aqua>Mostrando o raio de 8 blocos. <gray>Clique de novo para esconder." else "<gray>Raio escondido.")
    }

    override fun work(placed: PlacedBlock) {
        val block = placed.location.toBukkit()?.block ?: return
        val energy = EnergyNetwork.stored(placed)
        val next = SolarRules.produce(energy, TICKS, isActive(block))
        if (next == energy) return
        placed.counters[EnergyRules.KEY] = next
        PlacedBlockManager.markDirty(placed)
    }

    private fun isActive(block: Block): Boolean =
        SolarRules.isDay(block.world.time) && block.lightFromSky.toInt() == OPEN_SKY
}
