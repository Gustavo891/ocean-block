package ocean_block.system.machines.furnace

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.MachineMenus
import ocean_block.system.machines.energy.EnergyNetwork
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class FurnaceMenu : View() {

    private val blockState = initialState<PlacedBlock>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(MachineMenus.title("Fornalha elétrica"))
        config.scheduleUpdate(MachineMenus.UPDATE_TICKS)
        config.size(3)
    }

    override fun onFirstRender(render: RenderContext) {
        render.slot(2, 1).renderWith { statusItem(blockState.get(render)) }
        COLUMNS.forEachIndexed { index, column ->
            render.slot(1, column)
                .renderWith { inputItem(state(render).slots[index]) }
                .onClick { click -> take(click, index, output = false) }
            render.slot(2, column)
                .renderWith { progressItem(blockState.get(render), state(render).slots[index]) }
            render.slot(3, column)
                .renderWith { outputItem(state(render).slots[index]) }
                .onClick { click -> take(click, index, output = true) }
        }
    }

    override fun onClick(click: SlotClickContext) {
        val placed = blockState.get(click)
        MachineMenus.depositFromInventory(click, placed) { stack ->
            FurnaceBlock.insert(placed, stack.type.name, stack.amount)
        }
    }

    private fun state(render: RenderContext): FurnaceState = FurnaceState.fromCounters(blockState.get(render).counters)

    private fun take(click: SlotClickContext, index: Int, output: Boolean) {
        val placed = blockState.get(click)
        if (!MachineMenus.requireAccess(click, placed)) return
        val state = FurnaceState.fromCounters(placed.counters)
        val (next, item) = if (output) FurnaceRules.takeOutput(state, index) else FurnaceRules.takeInput(state, index)
        if (item == null) return
        FurnaceBlock.save(placed, next)
        PlacedBlockManager.saveNow(placed)
        MachineItems.give(click.player, listOf(item))
        click.update()
    }

    private fun inputItem(slot: FurnaceSlot): ItemStack {
        val stack = slot.input?.let { MachineItems.stackOf(it, slot.inputCount) }
            ?: return MachineMenus.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "<gray>Entrada vazia", listOf("<gray>Clique num item do seu", "<gray>inventário para colocar."))
        return MachineMenus.icon(stack.type, "<white>Entrada", listOf("<gray>Quantidade: <white>${slot.inputCount}", "", "<yellow>Clique para devolver."))
            .also { it.amount = stack.amount }
    }

    private fun outputItem(slot: FurnaceSlot): ItemStack {
        val stack = slot.output?.let { MachineItems.stackOf(it, slot.outputCount) }
            ?: return MachineMenus.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "<gray>Saída vazia", emptyList())
        return MachineMenus.icon(stack.type, "<white>Pronto", listOf("<gray>Quantidade: <white>${slot.outputCount}", "", "<yellow>Clique para coletar."))
            .also { it.amount = stack.amount }
    }

    private fun progressItem(placed: PlacedBlock, slot: FurnaceSlot): ItemStack =
        when (FurnaceRules.status(slot, FurnaceRecipes.results)) {
            FurnaceSlotStatus.EMPTY -> MachineMenus.icon(Material.GRAY_STAINED_GLASS_PANE, "<gray>Vazio", emptyList())
            FurnaceSlotStatus.OUTPUT_FULL -> MachineMenus.icon(Material.RED_STAINED_GLASS_PANE, "<red>Saída cheia", listOf("<gray>Colete o que está pronto."))
            FurnaceSlotStatus.WORKING ->
                if (EnergyNetwork.hasNoPower(placed)) MachineMenus.icon(Material.GRAY_DYE, "<red>Sem energia", listOf("<gray>Coloque um gerador com energia", "<gray>a até 8 blocos."))
                else MachineMenus.icon(Material.BLAZE_POWDER, "<gold>Derretendo ${slot.progress * 100 / FurnaceRules.TICKS_PER_ITEM}%", emptyList())
        }

    private fun statusItem(placed: PlacedBlock): ItemStack {
        val available = EnergyNetwork.available(placed)
        return MachineMenus.icon(
            if (EnergyNetwork.hasNoPower(placed)) Material.REDSTONE_TORCH else Material.LIGHTNING_ROD,
            "<yellow>Fornalha elétrica",
            listOf(
                "<gray>Energia no raio: <yellow>$available E",
                "",
                "<gray>Derrete 4 itens ao mesmo tempo.",
                "<gray>Cada item derretendo gasta <yellow>20 E/s<gray>.",
            ),
        )
    }

    companion object {
        private val COLUMNS = listOf(3, 5, 7, 9)
    }
}
