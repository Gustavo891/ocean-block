package ocean_block.system.machines.generator

import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.custom_items.CustomIds
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.MachineIds
import ocean_block.system.machines.MachineItems
import ocean_block.system.machines.energy.EnergyEffects
import ocean_block.system.machines.energy.EnergyNetwork
import ocean_block.system.machines.energy.EnergyRules
import ocean_block.system.stations.smelter.Fuel
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object GeneratorDialog {

    private const val SCREEN = "gerador"
    private const val BAR_WIDTH = 60
    private const val PER_SECOND = 20

    private val CONSUMERS = setOf(MachineIds.FURNACE, MachineIds.MINER)

    fun registerIcons() {
        DialogApi.registerItemIcons(Material.entries.filter { it.isItem && Fuel.ticks(it.name) != null }.map { iconFor(it.name) }.distinct())
    }

    fun open(player: Player, placed: PlacedBlock): Boolean {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, placed) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    private fun state(placed: PlacedBlock) = GeneratorState.fromCounters(placed.counters)

    private fun bind(binds: DialogBinds, player: Player, placed: PlacedBlock) {
        binds.text("status") { status(placed) }
        binds.text("estado") { if (state(placed).burnTicks > 0) "Queimando" else "Parado" }
        binds.text("energia") {
            "${NumberFormatter.format(state(placed).energy.toDouble())} / ${NumberFormatter.format(GeneratorRules.CAPACITY.toDouble())} E"
        }
        binds.text("barra_cheia") { GeneratorView.bar(state(placed).energy, GeneratorRules.CAPACITY, BAR_WIDTH).first }
        binds.text("barra_vazia") { GeneratorView.bar(state(placed).energy, GeneratorRules.CAPACITY, BAR_WIDTH).second }
        binds.text("gerando") { "${generating(state(placed))} E/s" }
        binds.text("gasto") { "${EnergyNetwork.spentPerSecond(placed)} E/s" }
        binds.text("maquinas") { consumersNear(placed).toString() }
        binds.text("raio_botao") { if (EnergyEffects.isShowingRadius(player, placed)) "Esconder raio" else "Mostrar raio" }
        binds.text("comb_item") { state(placed).fuel?.lowercase() ?: "" }
        binds.text("comb_qtd") { state(placed).fuelCount.coerceAtLeast(1).toString() }
        binds.text("comb_nome") { state(placed).fuel?.let(::fuelName) ?: "Sem combustível" }
        binds.text("comb_info") { fuelInfo(state(placed)) }
        binds.text("sem_comb") { (state(placed).fuel == null).toString() }
        binds.list("inventario") { inventoryFuels(player).map { mapOf("material" to iconFor(it.material), "qtd" to it.amount.toString(), "nome" to fuelName(it.material)) } }
        binds.listPage("inventario") { 0 }
        binds.button("raio") { click ->
            if (!MachineItems.canUse(click.player, placed)) return@button click.close()
            EnergyEffects.toggleRadius(click.player, placed)
        }
        binds.button("retirar") { click ->
            if (!MachineItems.canUse(click.player, placed)) return@button click.close()
            val (next, fuel) = GeneratorRules.takeFuel(state(placed))
            if (fuel == null) return@button
            GeneratorBlock.save(placed, next)
            PlacedBlockManager.saveNow(placed)
            MachineItems.give(click.player, listOf(fuel))
        }
        binds.listButton("colocar") { click, index ->
            if (!MachineItems.canUse(click.player, placed)) return@listButton click.close()
            val stack = inventoryFuels(click.player).getOrNull(index) ?: return@listButton
            val taken = GeneratorBlock.insertFuel(placed, stack.material, stack.amount)
            if (taken <= 0) return@listButton click.player.playSound(click.player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            removeFromInventory(click.player, stack.material, taken)
            click.player.playSound(click.player, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.8f, 1.2f)
        }
    }

    private fun generating(state: GeneratorState): Int =
        if (state.burnTicks > 0 && state.energy < GeneratorRules.CAPACITY) GeneratorRules.RATE * PER_SECOND else 0

    private fun status(placed: PlacedBlock): String {
        val state = state(placed)
        return when {
            state.energy >= GeneratorRules.CAPACITY -> "Bateria cheia, esperando gasto"
            state.burnTicks > 0 -> "Queima por mais ${duration(GeneratorView.burnSeconds(state, Fuel::ticks))}"
            state.fuel != null -> "Pronto para queimar"
            else -> "Coloque combustível para gerar"
        }
    }

    private fun fuelInfo(state: GeneratorState): String {
        val fuel = state.fuel ?: return "Até ${GeneratorRules.MAX_FUEL} de um tipo"
        val ticks = Fuel.ticks(fuel) ?: return ""
        return "${state.fuelCount}/${GeneratorRules.MAX_FUEL} · ${ticks * GeneratorRules.RATE} E cada"
    }

    private fun duration(seconds: Int): String =
        if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"

    private fun consumersNear(placed: PlacedBlock): Int {
        val center = placed.location.toBukkit() ?: return 0
        return PlacedBlockManager.all().count { other ->
            if (other.blockId !in CONSUMERS) return@count false
            val location = other.location.toBukkit() ?: return@count false
            location.world == center.world && location.distance(center) <= EnergyRules.RADIUS
        }
    }

    private fun inventoryFuels(player: Player): List<FuelStack> =
        GeneratorView.fuelEntries(
            player.inventory.storageContents.filterNotNull().filter(::isPlainItem).map { it.type.name to it.amount },
            Fuel::ticks,
        )

    private fun isPlainItem(item: ItemStack): Boolean = !item.type.isAir && CustomIds.of(item) == null

    private fun removeFromInventory(player: Player, material: String, amount: Int) {
        var left = amount
        val contents = player.inventory.storageContents
        contents.forEachIndexed { slot, item ->
            if (left == 0 || item == null || item.type.name != material || !isPlainItem(item)) return@forEachIndexed
            val take = minOf(left, item.amount)
            item.amount -= take
            left -= take
            player.inventory.setItem(slot, item.takeIf { it.amount > 0 })
        }
    }

    private fun iconFor(material: String): String = when {
        material == "DRIED_KELP_BLOCK" -> "dried_kelp"
        material.endsWith("_WOOD") -> material.removeSuffix("_WOOD").lowercase() + "_log"
        else -> material.lowercase()
    }

    private fun fuelName(material: String): String = when {
        material == "COAL" -> "Carvão"
        material == "CHARCOAL" -> "Carvão vegetal"
        material == "STICK" -> "Graveto"
        material == "DRIED_KELP_BLOCK" -> "Bloco de alga seca"
        material.endsWith("_PLANKS") -> "Tábuas"
        material.endsWith("_LOG") -> "Tronco"
        material.endsWith("_WOOD") -> "Madeira"
        else -> material.lowercase().replace('_', ' ')
    }
}
