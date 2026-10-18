package one_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.formatter.Mini
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.Items.createEmptyRender
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.translation.translate
import one_block.system.islands.monster.DropType
import one_block.system.islands.monster.MonsterConfig
import one_block.system.islands.phase.MOB_SPAWN_CHANCE_PERCENT
import one_block.system.islands.phase.MONSTER_PARTY_CHANCE_PERCENT
import one_block.system.islands.phase.MobSpawnData
import one_block.system.islands.phase.Phase
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.BooleanSupplier

class PhaseDetailView : View() {

    private val phaseState = initialState<Phase>()

    private val pagination = lazyPaginationState(
        { ctx -> buildEntries(phaseState.get(ctx)).toMutableList() }
    ) { context, builder, _, item ->
        builder.renderWith { item }

        if (item.type == Material.CHEST) {
            builder.onClick { click ->
                click.openForPlayer(ChestLootView::class.java, phaseState.get(context))
            }
        }
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
            openForPlayer(PhaseView::class.java)
        }
    }

    private fun buildEntries(phase: Phase): List<ItemStack> {
        val pesoBlocos = phase.blockPool.values.sum()
        val pesoMobs = phase.mobPool.values.sumOf { it.weight }

        val blocos = phase.blockPool.entries
            .sortedByDescending { it.value }
            .map { (material, peso) -> blockItem(material, peso, pesoBlocos, phase) }

        val mobs = phase.mobPool.entries
            .sortedByDescending { it.value.weight }
            .map { (type, data) -> mobItem(type, data, pesoMobs) }

        val chefes = phase.monsterParty?.let { party ->
            val pesoParty = party.sumOf { it.weight }
            party
                .sortedByDescending { it.weight }
                .map { bossItem(it, pesoParty) }
        }.orEmpty()

        return blocos + mobs + chefes
    }

    private fun summaryItem(phase: Phase) = itemStack(phase.iconMaterial) {
        val pesoBlocos = phase.blockPool.values.sum()
        val pesoBau = phase.blockPool[Material.CHEST] ?: 0

        meta<ItemMeta> {
            name("<yellow>${phase.prettyName}")
            addLore {
                +"<dark_gray>Blocos até a próxima fase: <gray>${NumberFormatter.format(phase.blocksToNextPhase.toDouble())}"
                +""
                +"<dark_gray>Tipos de bloco: <gray>${phase.blockPool.size}"
                +"<dark_gray>Tipos de mob: <gray>${phase.mobPool.size}"
                phase.chestLoot?.let { +"<dark_gray>Itens do baú: <gray>${it.entries.size}" }
                phase.monsterParty?.let { +"<dark_gray>Chefes: <gray>${it.size}" }
                +""
                +"<gray>Cada quebra gera um bloco novo."
                +"<gray>Mob aparece em <white>${percent(MOB_SPAWN_CHANCE_PERCENT)}<gray> das quebras."

                if (pesoBau > 0) {
                    +"<gray>Baú aparece em <white>${percent(chanceDe(pesoBau, pesoBlocos))}<gray> das quebras."
                }

                if (phase.monsterParty != null) {
                    +"<gray>Invasão em <white>${percent(MONSTER_PARTY_CHANCE_PERCENT)}<gray> das quebras."
                }
            }
        }
    }

    private fun bossItem(config: MonsterConfig, pesoTotal: Int) = itemStack(spawnEgg(config.entityType)) {
        meta<ItemMeta> {
            name(config.name ?: "<red>${translate(config.entityType.translationKey())}")

            val relativa = chanceDe(config.weight, pesoTotal)
            val absoluta = relativa * MONSTER_PARTY_CHANCE_PERCENT / 100.0

            addLore {
                +"<dark_gray>Chefe de invasão"
                +""
                +"<gray>Por quebra: <white>${percent(absoluta)}"
                +"<gray>Entre os chefes: <white>${percent(relativa)}"
                +"<dark_gray>Peso ${config.weight} de $pesoTotal"
                +""
                +"<gray>Quantidade: <white>${intervalo(config.min, config.max)}"

                config.attributes?.maxHealth?.let { +"<gray>Vida: <white>${it.toInt()}" }
                config.attributes?.attackDamage?.let { +"<gray>Dano: <white>${it.toInt()}" }

                if (config.drops.isNotEmpty()) {
                    +""
                    +"<white>Drops"
                    config.drops.forEach { drop ->
                        val rotulo = when (drop.type) {
                            DropType.ITEM -> {
                                val material = drop.material
                                if (material == null) "?" else "${drop.amount ?: 1}x ${blockLabel(material)}"
                            }

                            DropType.COMMAND -> "Recompensa especial"
                        }

                        +" <dark_gray>▪ <gray>$rotulo <dark_gray>(${percent(drop.chance)})"
                    }
                }
            }
        }
    }

    private fun blockItem(material: Material, peso: Int, pesoTotal: Int, phase: Phase) =
        itemStack(iconFor(material)) {
            val bau = material == Material.CHEST

            meta<ItemMeta> {
                name(if (bau) "<gold>Baú de Recompensa" else "<white>${blockLabel(material)}")
                addLore {
                    +if (bau) "<dark_gray>Baú" else "<dark_gray>Bloco"
                    +""
                    +"<gray>Chance: <white>${percent(chanceDe(peso, pesoTotal))}"
                    +"<dark_gray>Peso ${peso} de ${pesoTotal}"

                    val table = phase.chestLoot
                    if (bau && table != null) {
                        +""
                        +"<dark_gray>Itens possíveis: <gray>${table.entries.size}"
                        +"<dark_gray>Sorteios por baú: <gray>${intervalo(table.minRolls, table.maxRolls)}"
                        +""
                        +"<yellow>Clique para ver as recompensas."
                    }
                }
            }
        }

    private fun mobItem(type: EntityType, data: MobSpawnData, pesoTotal: Int) = itemStack(spawnEgg(type)) {
        meta<ItemMeta> {
            name("<white>${translate(type.translationKey())}")

            val relativa = chanceDe(data.weight, pesoTotal)
            val absoluta = relativa * MOB_SPAWN_CHANCE_PERCENT / 100.0

            addLore {
                +"<dark_gray>Mob"
                +""
                +"<gray>Por quebra: <white>${percent(absoluta)}"
                +"<gray>Entre os mobs: <white>${percent(relativa)}"
                +"<dark_gray>Peso ${data.weight} de $pesoTotal"
                +""
                +"<gray>Quantidade: <white>${quantidade(data)}"
            }
        }
    }

    private fun iconFor(material: Material): Material =
        if (material.isItem) material else Material.BUCKET

    private fun spawnEgg(type: EntityType): Material =
        Material.matchMaterial("${type.name}_SPAWN_EGG") ?: Material.EGG

    private fun chanceDe(peso: Int, pesoTotal: Int): Double =
        if (pesoTotal <= 0) 0.0 else peso * 100.0 / pesoTotal

    private fun percent(valor: Double): String = when {
        valor >= 10.0 -> String.format("%.1f%%", valor)
        valor >= 0.01 -> String.format("%.2f%%", valor)
        valor > 0.0 -> "<0,01%"
        else -> "0%"
    }

    private fun quantidade(data: MobSpawnData): String = intervalo(data.min, data.max)

    private fun intervalo(min: Int, max: Int): String =
        if (min == max) "$min" else "$min a $max"

    private fun arrowItem(texto: String) = itemStack(Material.ARROW) {
        meta<ItemMeta> { name(texto) }
    }
}
