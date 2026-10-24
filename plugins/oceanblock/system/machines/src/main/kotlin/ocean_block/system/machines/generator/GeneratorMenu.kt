package ocean_block.system.machines.generator

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.MachineMenus
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class GeneratorMenu : View() {

    private val blockState = initialState<PlacedBlock>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(MachineMenus.title("Gerador de energia"))
        config.scheduleUpdate(MachineMenus.UPDATE_TICKS)
        config.layout(
            "         ",
            "  F   S  ",
            "         ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('F')
            .renderWith { fuelItem(GeneratorState.fromCounters(blockState.get(render).counters)) }
            .onClick { click -> takeFuel(click) }
        render.layoutSlot('S')
            .renderWith { statusItem(GeneratorState.fromCounters(blockState.get(render).counters)) }
    }

    override fun onClick(click: SlotClickContext) {
        val placed = blockState.get(click)
        MachineMenus.depositFromInventory(click, placed) { stack ->
            GeneratorBlock.insertFuel(placed, stack.type.name, stack.amount)
        }
    }

    private fun takeFuel(click: SlotClickContext) {
        val placed = blockState.get(click)
        if (!MachineMenus.requireAccess(click, placed)) return
        val (next, fuel) = GeneratorRules.takeFuel(GeneratorState.fromCounters(placed.counters))
        if (fuel == null) return
        GeneratorBlock.save(placed, next)
        PlacedBlockManager.saveNow(placed)
        MachineItems.give(click.player, listOf(fuel))
        click.update()
    }

    private fun fuelItem(state: GeneratorState): ItemStack {
        val fuel = state.fuel?.let { MachineItems.stackOf(it, state.fuelCount) }
            ?: return MachineMenus.icon(
                Material.GRAY_STAINED_GLASS_PANE,
                "<gray>Sem combustível",
                listOf("<gray>Clique num combustível do", "<gray>seu inventário para colocar."),
            )
        return MachineMenus.icon(fuel.type, "<white>Combustível", listOf("<gray>Guardado: <white>${state.fuelCount}/${GeneratorRules.MAX_FUEL}", "", "<yellow>Clique para retirar."))
            .also { it.amount = fuel.amount }
    }

    private fun statusItem(state: GeneratorState): ItemStack {
        val burning = state.burnTicks > 0
        return MachineMenus.icon(
            if (burning) Material.BLAZE_POWDER else Material.GUNPOWDER,
            "<yellow>Gerador de energia",
            listOf(
                MachineItems.energyLine(state.energy, GeneratorRules.CAPACITY),
                if (burning) "<green>Queimando: <white>${state.burnTicks / 20}s" else "<red>Parado",
                "",
                "<gray>Gera <yellow>40 E/s <gray>enquanto queima.",
                "<gray>Alimenta máquinas a até <white>8 blocos<gray>.",
            ),
        )
    }
}
