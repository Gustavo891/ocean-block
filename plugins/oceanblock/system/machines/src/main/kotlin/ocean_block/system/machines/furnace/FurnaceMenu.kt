package ocean_block.system.machines.furnace

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.MachineMenus
import ocean_block.system.machines.energy.EnergyNetwork
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class FurnaceMenu : View() {

    private val blockState = initialState<PlacedBlock>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(Mini.mm.deserialize(TITLE))
        config.scheduleUpdate(MachineMenus.UPDATE_TICKS)
        config.size(ROWS)
    }

    override fun onFirstRender(render: RenderContext) {
        LANE_COLUMNS.forEachIndexed { lane, column ->
            render.slot(INPUT_ROW, column)
                .renderWith { inputItem(blockState.get(render), state(render).slots[lane]) }
                .onClick { click -> take(click, lane, output = false) }
            render.slot(OUTPUT_ROW, column)
                .renderWith { outputItem(blockState.get(render), state(render).slots[lane]) }
                .onClick { click -> take(click, lane, output = true) }
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

    private fun inputItem(placed: PlacedBlock, slot: FurnaceSlot): ItemStack {
        val input = slot.input?.takeIf { slot.inputCount > 0 }
            ?: return blank("<gray>Entrada vazia", listOf("<gray>Clique num item do seu", "<gray>inventário para colocar."))
        val lore = listOfNotNull(
            FurnaceRecipes.results[input]?.let { "<gray>Vira ${lang(it)}" },
            "<gray>Na fila: <white>${slot.inputCount}",
            statusLine(placed, slot),
            "",
            "<yellow>Clique para devolver.",
        )
        return MachineMenus.icon(material(input), "<white>${lang(input)}", lore).also { it.amount = slot.inputCount.coerceIn(1, 64) }
    }

    private fun outputItem(placed: PlacedBlock, slot: FurnaceSlot): ItemStack {
        val output = slot.output?.takeIf { slot.outputCount > 0 }
        if (output != null) {
            return MachineMenus.icon(
                material(output),
                "<white>${lang(output)}",
                listOfNotNull("<gray>Pronto: <white>${slot.outputCount}", statusLine(placed, slot), "", "<yellow>Clique para recolher."),
            ).also { it.amount = slot.outputCount.coerceIn(1, 64) }
        }
        if (FurnaceRules.status(slot, FurnaceRecipes.results) == FurnaceSlotStatus.EMPTY) return blank("<gray>Resultado", listOf("<gray>O item derretido aparece aqui."))
        val fill = FurnaceBar.segmentFill(slot.progress, FurnaceRules.TICKS_PER_ITEM, 0, 1)
        return model(NamespacedKey("oneblock", "barra_$fill"), "<gray>Resultado", listOfNotNull(statusLine(placed, slot)))
    }

    private fun statusLine(placed: PlacedBlock, slot: FurnaceSlot): String? = when (FurnaceRules.status(slot, FurnaceRecipes.results)) {
        FurnaceSlotStatus.EMPTY -> null
        FurnaceSlotStatus.OUTPUT_FULL -> "<red>Saída cheia, recolha."
        FurnaceSlotStatus.WORKING ->
            if (EnergyNetwork.hasNoPower(placed)) "<red>Sem energia: gerador a até 8 blocos."
            else "<gold>Derretendo ${slot.progress * 100 / FurnaceRules.TICKS_PER_ITEM}%"
    }

    private fun blank(title: String, lore: List<String>): ItemStack = model(BLANK, title, lore)

    private fun model(key: NamespacedKey, title: String, lore: List<String>): ItemStack = itemStack(Material.PAPER) {
        meta {
            itemModel = key
            name(title)
            setLore { lore.forEach { +it } }
            addItemFlags(*ItemFlag.entries.toTypedArray())
        }
    }

    private fun material(name: String): Material = Material.matchMaterial(name) ?: Material.BARRIER

    private fun lang(material: String): String =
        Material.matchMaterial(material)?.let { "<lang:${it.translationKey()}>" } ?: material.lowercase()

    companion object {
        private const val TITLE = "<white><shift:-12><glyph:fornalha_menu>"
        private const val ROWS = 2
        private const val INPUT_ROW = 1
        private const val OUTPUT_ROW = 2
        private val LANE_COLUMNS = listOf(2, 4, 6, 8)
        private val BLANK = NamespacedKey("oneblock", "blank")
    }
}
