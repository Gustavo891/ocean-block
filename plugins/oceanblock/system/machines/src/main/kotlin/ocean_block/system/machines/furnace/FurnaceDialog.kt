package ocean_block.system.machines.furnace

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.ItemLabels
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.energy.EnergyNetwork
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.UUID

object FurnaceDialog {

    const val REFRESH_TICKS = 10L
    private const val SCREEN = "fornalha"
    private const val ENERGY_PER_SECOND = 20
    private val INVENTORY_SLOTS = (9..35) + (0..8)
    private val EMPTY_CELL = mapOf("item" to "", "qtd" to "1", "nome" to "", "dica" to "")

    private val viewers = mutableMapOf<UUID, UUID>()

    fun registerIcons() {
        val recipes = FurnaceRecipes.results
        DialogApi.registerItemIcons((recipes.keys + recipes.values).map(ItemLabels::icon).distinct())
    }

    fun open(player: Player, placed: PlacedBlock): Boolean {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, placed) }
        val shown = result == DialogOpenResult.Shown
        if (shown) viewers[player.uniqueId] = placed.uuid
        return shown || result == DialogOpenResult.NeedsPack
    }

    fun refresh() {
        viewers.entries.removeIf { (playerId, placedId) ->
            val player = Bukkit.getPlayer(playerId)
            val placed = PlacedBlockManager.get(placedId)
            when {
                player == null || !player.isOnline -> true
                placed == null || !PlacedBlockManager.isRegistered(placed) -> {
                    DialogApi.close(player)
                    true
                }
                player.openInventory.topInventory.type != InventoryType.CRAFTING -> true
                else -> !DialogApi.refresh(player, SCREEN)
            }
        }
    }

    private fun state(placed: PlacedBlock) = FurnaceState.fromCounters(placed.counters)

    private fun working(placed: PlacedBlock): Int =
        state(placed).slots.count { FurnaceRules.status(it, FurnaceRecipes.results) == FurnaceSlotStatus.WORKING }

    private fun bind(binds: DialogBinds, player: Player, placed: PlacedBlock) {
        binds.text("estado") { stateLabel(placed).first }
        binds.text("estado_estilo") { stateLabel(placed).second }
        binds.text("status") { status(placed) }
        binds.text("gasto") { "${working(placed) * ENERGY_PER_SECOND} E/s" }
        binds.list("fila") { state(placed).slots.map { queueEntry(placed, it) } }
        binds.listPage("fila") { 0 }
        binds.text("prontos") { state(placed).slots.sumOf { it.outputCount }.toString() }
        binds.text("sem_saida") { state(placed).slots.none { it.outputCount > 0 }.toString() }
        binds.list("inventario") { padded(smeltable(player).map { inventoryEntry(player.inventory.getItem(it)) }) }
        binds.listPage("inventario") { 0 }
        binds.text("inv_info") { if (smeltable(player).isEmpty()) "Nada para derreter." else "" }
        binds.text("sem_derretiveis") { smeltable(player).isEmpty().toString() }
        binds.listButton("cancelar") { click, index -> take(click.player, placed, index, output = false) }
        binds.listButton("recolher") { click, index -> take(click.player, placed, index, output = true) }
        binds.button("recolher_tudo") { click ->
            (0 until FurnaceRules.SLOTS).forEach { take(click.player, placed, it, output = true) }
        }
        binds.listButton("colocar") { click, index -> smeltable(click.player).getOrNull(index)?.let { insert(click.player, placed, it) } }
        binds.button("colocar_tudo") { click -> smeltable(click.player).forEach { insert(click.player, placed, it, quiet = true) } }
    }

    private fun smeltable(player: Player): List<Int> = INVENTORY_SLOTS.filter { isSmeltable(player.inventory.getItem(it)) }

    private fun isSmeltable(stack: ItemStack?): Boolean =
        stack != null && MachineItems.isVanilla(stack) && stack.type.name in FurnaceRecipes.results

    private fun take(player: Player, placed: PlacedBlock, index: Int, output: Boolean) {
        if (!MachineItems.canUse(player, placed)) return DialogApi.close(player)
        val current = state(placed)
        val (next, item) = if (output) FurnaceRules.takeOutput(current, index) else FurnaceRules.takeInput(current, index)
        if (item == null) return
        FurnaceBlock.save(placed, next)
        PlacedBlockManager.saveNow(placed)
        MachineItems.give(player, listOf(item))
    }

    private fun insert(player: Player, placed: PlacedBlock, slot: Int, quiet: Boolean = false) {
        if (!MachineItems.canUse(player, placed)) return DialogApi.close(player)
        val stack = player.inventory.getItem(slot) ?: return
        if (!isSmeltable(stack)) {
            if (!quiet) player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            return
        }
        val taken = FurnaceBlock.insert(placed, stack.type.name, stack.amount)
        if (taken <= 0) {
            if (!quiet) player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            return
        }
        stack.amount -= taken
        player.inventory.setItem(slot, stack.takeIf { it.amount > 0 })
        player.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.6f)
    }

    private fun padded(entries: List<Map<String, String>>): List<Map<String, String>> =
        entries.take(INVENTORY_SLOTS.size) + List((INVENTORY_SLOTS.size - entries.size).coerceAtLeast(0)) { EMPTY_CELL }

    private fun inventoryEntry(stack: ItemStack?): Map<String, String> {
        val material = stack?.type?.name ?: return EMPTY_CELL
        val result = FurnaceRecipes.results[material]
        return mapOf(
            "item" to ItemLabels.icon(material),
            "qtd" to stack.amount.toString(),
            "nome" to ItemLabels.name(material),
            "dica" to (result?.let { "Vira ${ItemLabels.name(it)}. Clique para colocar." } ?: ""),
        )
    }

    private fun queueEntry(placed: PlacedBlock, slot: FurnaceSlot): Map<String, String> {
        val input = slot.input?.takeIf { slot.inputCount > 0 }
        val result = input?.let { FurnaceRecipes.results[it] } ?: slot.output
        return mapOf(
            "in" to (input?.let(ItemLabels::icon) ?: ""),
            "in_qtd" to slot.inputCount.coerceAtLeast(1).toString(),
            "in_nome" to (input?.let(ItemLabels::name) ?: ""),
            "out" to (slot.output?.takeIf { slot.outputCount > 0 }?.let(ItemLabels::icon) ?: ""),
            "out_qtd" to slot.outputCount.coerceAtLeast(1).toString(),
            "out_nome" to (slot.output?.let(ItemLabels::name) ?: ""),
            "nome" to (result?.let(ItemLabels::name) ?: "Vaga livre"),
            "prog" to (if (input != null) slot.progress else 0).toString(),
            "info" to queueInfo(placed, slot, input),
            "vazio" to (input == null).toString(),
        )
    }

    private fun queueInfo(placed: PlacedBlock, slot: FurnaceSlot, input: String?): String = when {
        input == null && slot.outputCount > 0 -> "Pronto"
        input == null -> ""
        FurnaceRules.status(slot, FurnaceRecipes.results) == FurnaceSlotStatus.OUTPUT_FULL -> "Cheia"
        EnergyNetwork.hasNoPower(placed) -> "Sem energia"
        else -> "x${slot.inputCount} ${FurnaceClock.format(FurnaceClock.totalSeconds(slot.progress, slot.inputCount, FurnaceRules.TICKS_PER_ITEM))}"
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
            count == 1 -> "Derretendo 1 de ${FurnaceRules.SLOTS} vagas"
            count > 1 -> "Derretendo $count de ${FurnaceRules.SLOTS} vagas"
            else -> "Coloque itens para derreter"
        }
    }
}
