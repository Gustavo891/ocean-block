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
import org.bukkit.Sound
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
        repeat(FurnaceRules.SLOTS) { lane ->
            val row = lane + 1
            render.slot(row, INPUT_COLUMN)
                .renderWith { inputItem(state(render).slots[lane]) }
                .onClick { click -> take(click, lane, output = false) }
            BAR_COLUMNS.forEachIndexed { segment, column ->
                render.slot(row, column).renderWith { segmentItem(blockState.get(render), state(render).slots[lane], segment) }
            }
            render.slot(row, OUTPUT_COLUMN)
                .renderWith { outputItem(state(render).slots[lane]) }
                .onClick { click -> take(click, lane, output = true) }
        }
        render.slot(ROWS, STATUS_COLUMN).renderWith { statusItem(blockState.get(render)) }
        render.slot(ROWS, COLLECT_COLUMN)
            .renderWith { collectItem(state(render)) }
            .onClick { click -> repeat(FurnaceRules.SLOTS) { take(click, it, output = true) } }
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
        val input = slot.input?.takeIf { slot.inputCount > 0 }
            ?: return blank("<gray>Entrada vazia", listOf("<gray>Clique num item do seu", "<gray>inventário para colocar."))
        val result = FurnaceRecipes.results[input]
        val lore = listOfNotNull(
            result?.let { "<gray>Vira ${lang(it)}" },
            "<gray>Na fila: <white>${slot.inputCount}",
            "",
            "<yellow>Clique para devolver.",
        )
        return MachineMenus.icon(material(input), "<white>${lang(input)}", lore).also { it.amount = slot.inputCount.coerceIn(1, 64) }
    }

    private fun outputItem(slot: FurnaceSlot): ItemStack {
        val output = slot.output?.takeIf { slot.outputCount > 0 } ?: return blank("<gray>Saída vazia", emptyList())
        return MachineMenus.icon(
            material(output),
            "<white>${lang(output)}",
            listOf("<gray>Pronto: <white>${slot.outputCount}", "", "<yellow>Clique para recolher."),
        ).also { it.amount = slot.outputCount.coerceIn(1, 64) }
    }

    private fun segmentItem(placed: PlacedBlock, slot: FurnaceSlot, segment: Int): ItemStack {
        val status = FurnaceRules.status(slot, FurnaceRecipes.results)
        val fill = FurnaceBar.segmentFill(slot.progress, FurnaceRules.TICKS_PER_ITEM, segment, BAR_COLUMNS.size)
            .takeIf { status != FurnaceSlotStatus.EMPTY } ?: 0
        val (title, lore) = when {
            status == FurnaceSlotStatus.EMPTY -> "<gray>Espaço livre" to listOf("<gray>Clique num item do seu", "<gray>inventário para colocar.")
            status == FurnaceSlotStatus.OUTPUT_FULL -> "<red>Saída cheia" to listOf("<gray>Recolha o que está pronto.")
            EnergyNetwork.hasNoPower(placed) -> "<red>Sem energia" to listOf("<gray>Coloque um gerador com energia", "<gray>a até 8 blocos.")
            else -> "<gold>Derretendo <white>${slot.progress * 100 / FurnaceRules.TICKS_PER_ITEM}%" to listOfNotNull(
                slot.input?.let { input -> "<gray>${lang(input)} <dark_gray>> <gray>${FurnaceRecipes.results[input]?.let(::lang) ?: "?"}" },
                "<gray>Na fila: <white>${slot.inputCount}",
            )
        }
        return model(NamespacedKey("oneblock", "barra_$fill"), title, lore)
    }

    private fun statusItem(placed: PlacedBlock): ItemStack {
        val working = FurnaceState.fromCounters(placed.counters).slots.count { FurnaceRules.status(it, FurnaceRecipes.results) == FurnaceSlotStatus.WORKING }
        val noPower = working > 0 && EnergyNetwork.hasNoPower(placed)
        return MachineMenus.icon(
            if (noPower) Material.REDSTONE_TORCH else Material.LIGHTNING_ROD,
            if (noPower) "<red>Sem energia" else "<yellow>Fornalha elétrica",
            listOf(
                "<gray>Derretendo: <white>$working de ${FurnaceRules.SLOTS}",
                "<gray>Gasto: <yellow>${working * ENERGY_PER_SECOND} E/s",
                "<gray>Energia no raio: <yellow>${EnergyNetwork.available(placed)} E",
                "",
                "<gray>Clique num item do seu inventário",
                "<gray>para colocar na fila.",
            ),
        )
    }

    private fun collectItem(state: FurnaceState): ItemStack {
        val ready = state.slots.sumOf { it.outputCount }
        return MachineMenus.icon(
            Material.HOPPER,
            if (ready > 0) "<green>Recolher tudo" else "<gray>Recolher tudo",
            listOf(if (ready > 0) "<gray>Prontos: <white>$ready" else "<gray>Nada pronto ainda.", "", "<yellow>Clique para recolher."),
        )
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
        private const val TITLE = "<white><shift:-8><glyph:fornalha_menu><font:oneblock:gui><shift:-170><white>Fornalha elétrica"
        private const val ROWS = 5
        private const val INPUT_COLUMN = 1
        private const val OUTPUT_COLUMN = 8
        private const val STATUS_COLUMN = 1
        private const val COLLECT_COLUMN = 5
        private const val ENERGY_PER_SECOND = 20
        private val BAR_COLUMNS = listOf(2, 3, 4, 5, 6)
        private val BLANK = NamespacedKey("oneblock", "blank")
    }
}
