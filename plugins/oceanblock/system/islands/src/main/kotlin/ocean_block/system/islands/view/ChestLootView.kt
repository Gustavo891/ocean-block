package ocean_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.Items.createEmptyRender
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.islands.phase.ChestLootEntry
import ocean_block.system.islands.phase.Phase
import one_block.textura.api.ItemApi
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.BooleanSupplier

class ChestLootView : View() {

    private val phaseState = initialState<Phase>()

    private val pagination = lazyPaginationState(
        { ctx -> buildEntries(phaseState.get(ctx)).toMutableList() }
    ) { _, builder, _, item ->
        builder.renderWith { item }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(Mini.deserialize(""))
        config.layout(
            "    i    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " <  v  > "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = pagination.get(render)

        render.layoutSlot('i').renderWith { summaryItem(phaseState.get(render)) }

        render.layoutSlot('<')
            .updateOnStateChange(this.pagination)
            .renderWith { arrowItem("<yellow>◀ Página anterior") }
            .displayIf(BooleanSupplier { pagination.canBack() })
            .onClick(pagination::back)

        render.layoutSlot('>')
            .updateOnStateChange(this.pagination)
            .renderWith { arrowItem("<yellow>Próxima página ▶") }
            .displayIf(BooleanSupplier { pagination.canAdvance() })
            .onClick(pagination::advance)

        render.createEmptyRender('v') {
            openForPlayer(PhaseDetailView::class.java, phaseState.get(render))
        }
    }

    private fun buildEntries(phase: Phase): List<ItemStack> {
        val table = phase.chestLoot ?: return emptyList()
        val pesoTotal = table.entries.sumOf { it.weight }

        return table.entries
            .sortedByDescending { it.weight }
            .mapNotNull { lootItem(it, pesoTotal) }
    }

    private fun summaryItem(phase: Phase) = itemStack(Material.CHEST) {
        val table = phase.chestLoot
        val pesoBlocos = phase.blockPool.values.sum()
        val pesoBau = phase.blockPool[Material.CHEST] ?: 0

        meta<ItemMeta> {
            name("<gold>Baú de ${phase.prettyName}")
            addLore {
                if (table == null) {
                    +"<gray>Esta fase não tem baú de recompensa."
                    return@addLore
                }

                +"<dark_gray>Itens possíveis: <gray>${table.entries.size}"
                +"<dark_gray>Sorteios por baú: <gray>${intervalo(table.minRolls, table.maxRolls)}"
                +""

                if (pesoBau > 0) {
                    +"<gray>Aparece em <white>${percent(chanceDe(pesoBau, pesoBlocos))}<gray> das quebras."
                }

                +"<gray>Cada sorteio escolhe um item pelo peso."
            }
        }
    }

    private fun lootItem(entry: ChestLootEntry, pesoTotal: Int): ItemStack? {
        val base = entry.texturaId?.let { ItemApi.create(it) }
            ?: entry.material?.let { ItemStack(iconFor(it)) }
            ?: return null

        base.editMeta { meta ->
            entry.material?.let { meta.name("<white>${blockLabel(it)}") }

            meta.addLore {
                +"<dark_gray>Recompensa do baú"
                +""
                +"<gray>Chance por sorteio: <white>${percent(chanceDe(entry.weight, pesoTotal))}"
                +"<dark_gray>Peso ${entry.weight} de $pesoTotal"
                +""
                +"<gray>Quantidade: <white>${intervalo(entry.min, entry.max)}"
            }
        }

        return base
    }

    private fun iconFor(material: Material): Material =
        if (material.isItem) material else Material.BUCKET

    private fun chanceDe(peso: Int, pesoTotal: Int): Double =
        if (pesoTotal <= 0) 0.0 else peso * 100.0 / pesoTotal

    private fun percent(valor: Double): String = when {
        valor >= 10.0 -> String.format("%.1f%%", valor)
        valor >= 0.01 -> String.format("%.2f%%", valor)
        valor > 0.0 -> "<0,01%"
        else -> "0%"
    }

    private fun intervalo(min: Int, max: Int): String =
        if (min == max) "$min" else "$min a $max"

    private fun arrowItem(texto: String) = itemStack(Material.ARROW) {
        meta<ItemMeta> { name(texto) }
    }
}
