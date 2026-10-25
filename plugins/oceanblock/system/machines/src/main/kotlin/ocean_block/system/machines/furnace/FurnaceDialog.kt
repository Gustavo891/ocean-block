package ocean_block.system.machines.furnace

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.ItemLabels
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.energy.EnergyNetwork
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object FurnaceDialog {

    private const val SCREEN = "fornalha"
    private const val LOCKED_ICON = "ui:bloqueado"
    private const val ENERGY_PER_SECOND = 20
    private val BACKPACK = 9..35
    private val HOTBAR = 0..8

    fun registerIcons() {
        val recipes = FurnaceRecipes.results
        DialogApi.registerItemIcons((recipes.keys + recipes.values).map(ItemLabels::icon).distinct())
    }

    fun open(player: Player, placed: PlacedBlock): Boolean {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, placed) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    private fun state(placed: PlacedBlock) = FurnaceState.fromCounters(placed.counters)

    private fun working(placed: PlacedBlock): Int =
        state(placed).slots.count { FurnaceRules.status(it, FurnaceRecipes.results) == FurnaceSlotStatus.WORKING }

    private fun bind(binds: DialogBinds, player: Player, placed: PlacedBlock) {
        binds.text("estado") { stateLabel(placed).first }
        binds.text("estado_estilo") { stateLabel(placed).second }
        binds.text("status") { status(placed) }
        binds.text("gasto") { "Gasto: ${working(placed) * ENERGY_PER_SECOND} E/s" }
        binds.list("fila") { state(placed).slots.map(::queueEntry) }
        binds.listPage("fila") { 0 }
        binds.list("inventario") { BACKPACK.map { inventoryEntry(player.inventory.getItem(it)) } }
        binds.listPage("inventario") { 0 }
        binds.list("hotbar") { HOTBAR.map { inventoryEntry(player.inventory.getItem(it)) } }
        binds.listPage("hotbar") { 0 }
        binds.text("sem_saida") { state(placed).slots.none { it.output != null }.toString() }
        binds.listButton("devolver") { click, index -> take(click.player, placed, index, output = false) }
        binds.listButton("recolher") { click, index -> take(click.player, placed, index, output = true) }
        binds.button("recolher_tudo") { click ->
            (0 until FurnaceRules.SLOTS).forEach { take(click.player, placed, it, output = true) }
        }
        binds.listButton("colocar") { click, index -> insert(click.player, placed, BACKPACK.first + index) }
        binds.listButton("colocar_barra") { click, index -> insert(click.player, placed, HOTBAR.first + index) }
    }

    private fun take(player: Player, placed: PlacedBlock, index: Int, output: Boolean) {
        if (!MachineItems.canUse(player, placed)) return DialogApi.close(player)
        val current = state(placed)
        val (next, item) = if (output) FurnaceRules.takeOutput(current, index) else FurnaceRules.takeInput(current, index)
        if (item == null) return
        FurnaceBlock.save(placed, next)
        PlacedBlockManager.saveNow(placed)
        MachineItems.give(player, listOf(item))
    }

    private fun insert(player: Player, placed: PlacedBlock, slot: Int) {
        if (!MachineItems.canUse(player, placed)) return DialogApi.close(player)
        val stack = player.inventory.getItem(slot)?.takeIf(MachineItems::isVanilla) ?: return
        val taken = FurnaceBlock.insert(placed, stack.type.name, stack.amount)
        if (taken <= 0) return player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
        stack.amount -= taken
        player.inventory.setItem(slot, stack.takeIf { it.amount > 0 })
        player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.6f)
    }

    private fun inventoryEntry(stack: ItemStack?): Map<String, String> {
        if (stack == null || stack.type.isAir) return mapOf("item" to "", "qtd" to "1", "nome" to "", "dica" to "", "nao" to "false")
        val material = stack.type.name
        val result = FurnaceRecipes.results[material]?.takeIf { MachineItems.isVanilla(stack) }
        return mapOf(
            "item" to (result?.let { ItemLabels.icon(material) } ?: LOCKED_ICON),
            "qtd" to (if (result == null) 1 else stack.amount).toString(),
            "nome" to ItemLabels.name(material),
            "dica" to (result?.let { "Vira ${ItemLabels.name(it)}. Clique para colocar." } ?: "Não derrete aqui."),
            "nao" to (result == null).toString(),
        )
    }

    private fun queueEntry(slot: FurnaceSlot): Map<String, String> {
        val input = slot.input?.takeIf { slot.inputCount > 0 }
        val result = input?.let { FurnaceRecipes.results[it] }
        return mapOf(
            "in" to (input?.let(ItemLabels::icon) ?: ""),
            "in_qtd" to slot.inputCount.coerceAtLeast(1).toString(),
            "in_nome" to (input?.let(ItemLabels::name) ?: ""),
            "out" to (slot.output?.let(ItemLabels::icon) ?: ""),
            "out_qtd" to slot.outputCount.coerceAtLeast(1).toString(),
            "out_nome" to (slot.output?.let(ItemLabels::name) ?: ""),
            "nome" to (input?.let { "${ItemLabels.name(it)} > ${result?.let(ItemLabels::name) ?: "?"}" } ?: "Espaço livre"),
            "prog" to (if (input != null) slot.progress else 0).toString(),
            "info" to queueInfo(slot, input),
        )
    }

    private fun queueInfo(slot: FurnaceSlot, input: String?): String = when {
        input == null && slot.output != null -> "Pronto para recolher"
        input == null -> "Clique num item do inventário"
        FurnaceRules.status(slot, FurnaceRecipes.results) == FurnaceSlotStatus.OUTPUT_FULL -> "Saída cheia, recolha"
        else -> "${slot.inputCount} na fila"
    }

    private fun stateLabel(placed: PlacedBlock): Pair<String, String> {
        val slots = state(placed).slots
        return when {
            working(placed) > 0 && EnergyNetwork.hasNoPower(placed) -> "SEM ENERGIA" to "bad"
            working(placed) > 0 -> "DERRETENDO" to "good"
            slots.any { FurnaceRules.status(it, FurnaceRecipes.results) == FurnaceSlotStatus.OUTPUT_FULL } -> "SAÍDA CHEIA" to "accent"
            else -> "PARADA" to "muted"
        }
    }

    private fun status(placed: PlacedBlock): String {
        val count = working(placed)
        return when {
            count > 0 && EnergyNetwork.hasNoPower(placed) -> "Sem gerador a até 8 blocos"
            count == 1 -> "Derretendo 1 item"
            count > 1 -> "Derretendo $count itens"
            else -> "Coloque itens para derreter"
        }
    }
}
