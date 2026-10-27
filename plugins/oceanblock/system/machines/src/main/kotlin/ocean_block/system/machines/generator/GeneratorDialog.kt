package ocean_block.system.machines.generator

import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.ItemLabels
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.energy.EnergyEffects
import ocean_block.system.machines.energy.EnergyNetwork
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.Material
import org.bukkit.entity.Player

object GeneratorDialog {

    private const val SCREEN = "gerador"
    private const val PER_SECOND = 20

    private val MACHINE_ICONS = mapOf(
        MachineIds.FURNACE to ("ui:fornalha_eletrica" to "Fornalha elétrica"),
        MachineIds.MINER to ("ui:minerador" to "Minerador"),
    )

    fun registerIcons() {
        DialogApi.registerItemIcons(Material.entries.filter { it.isItem && Fuel.ticks(it.name) != null }.map { ItemLabels.icon(it.name) }.distinct())
    }

    fun open(player: Player, placed: PlacedBlock): Boolean {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, placed) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    private fun state(placed: PlacedBlock) = GeneratorState.fromCounters(placed.counters)

    private fun bind(binds: DialogBinds, player: Player, placed: PlacedBlock) {
        binds.text("status") { status(placed) }
        binds.text("estado") { stateLabel(state(placed)).first }
        binds.text("estado_estilo") { stateLabel(state(placed)).second }
        binds.text("autonomia") { autonomy(state(placed)) }
        binds.text("energia") {
            "${NumberFormatter.format(state(placed).energy.toDouble())} / ${NumberFormatter.format(GeneratorRules.CAPACITY.toDouble())} E"
        }
        binds.text("bateria_nivel") { GeneratorView.batteryLevel(state(placed).energy, GeneratorRules.CAPACITY).toString() }
        binds.text("energia_pct") { GeneratorView.percent(state(placed).energy, GeneratorRules.CAPACITY).toString() }
        binds.text("gerando") { "${generating(state(placed))} E/s" }
        binds.text("gasto") { "${EnergyNetwork.spentPerSecond(placed)} E/s" }
        binds.text("maquinas_info") { machinesInfo(EnergyNetwork.consumersOf(placed).size) }
        binds.list("maquinas") { EnergyNetwork.consumersOf(placed).mapNotNull(::machineEntry) }
        binds.listPage("maquinas") { 0 }
        binds.switch("raio", { EnergyEffects.isShowingRadius(player, placed) }) { show ->
            if (MachineItems.canUse(player, placed) && show != EnergyEffects.isShowingRadius(player, placed)) EnergyEffects.toggleRadius(player, placed)
        }
        binds.list("combustivel") { state(placed).slots.map(::fuelEntry) }
        binds.listPage("combustivel") { 0 }
        binds.listButton("retirar") { click, index ->
            if (!MachineItems.canUse(click.player, placed)) return@listButton click.close()
            val (next, fuel) = GeneratorRules.takeSlot(state(placed), index)
            if (fuel == null) return@listButton
            GeneratorBlock.save(placed, next)
            PlacedBlockManager.saveNow(placed)
            MachineItems.give(click.player, listOf(fuel.material to fuel.amount))
        }
    }

    private fun fuelEntry(stack: FuelStack?): Map<String, String> = mapOf(
        "item" to (stack?.let { ItemLabels.icon(it.material) } ?: ""),
        "qtd" to (stack?.amount ?: 1).toString(),
        "nome" to (stack?.let { ItemLabels.name(it.material) } ?: ""),
        "info" to (stack?.let(::fuelInfo) ?: ""),
    )

    private fun machineEntry(consumer: PlacedBlock): Map<String, String>? {
        val (icon, name) = MACHINE_ICONS[consumer.blockId] ?: return null
        val location = consumer.location
        return mapOf("icone" to icon, "nome" to name, "coord" to "X ${location.x.toInt()}, Y ${location.y.toInt()}, Z ${location.z.toInt()}")
    }

    private fun machinesInfo(count: Int): String = when (count) {
        0 -> "Nenhuma máquina puxando energia"
        1 -> "1 máquina puxando energia:"
        else -> "$count máquinas puxando energia:"
    }

    private fun generating(state: GeneratorState): Int =
        if (state.burnTicks > 0 && state.energy < GeneratorRules.CAPACITY) GeneratorRules.RATE * PER_SECOND else 0

    private fun status(placed: PlacedBlock): String {
        val state = state(placed)
        return when {
            state.energy >= GeneratorRules.CAPACITY -> "Bateria cheia, esperando gasto"
            state.burnTicks > 0 -> "Queima por mais ${duration(GeneratorView.burnSeconds(state, Fuel::ticks))}"
            state.hasFuel -> "Pronto para queimar"
            else -> "Coloque alga seca ou prismarinho"
        }
    }

    private fun stateLabel(state: GeneratorState): Pair<String, String> = when {
        state.energy >= GeneratorRules.CAPACITY -> "CHEIO" to "accent"
        state.burnTicks > 0 -> "QUEIMANDO" to "good"
        else -> "PARADO" to "muted"
    }

    private fun autonomy(state: GeneratorState): String {
        val seconds = GeneratorView.burnSeconds(state, Fuel::ticks)
        return if (seconds > 0) "Autonomia ${duration(seconds)}" else "Vazio"
    }

    private fun fuelInfo(stack: FuelStack): String {
        val ticks = Fuel.ticks(stack.material) ?: return "Não queima mais."
        return "${stack.amount}/${GeneratorRules.MAX_STACK} - ${ticks * GeneratorRules.RATE} E cada"
    }

    private fun duration(seconds: Int): String =
        if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"

}
