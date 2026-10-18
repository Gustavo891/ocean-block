package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.formatter.Mini
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopperUpgrade
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

class VacuumHopperView : View() {

    private val blockState = initialState<PlacedBlock>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:coletor_itens>")
        )
        config.layout(
            "  T I SU "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placed = blockState.get(render)

        render.layoutSlot('I').renderWith { statusItem(placed) }

        render.layoutSlot('T')
            .renderWith { toggleItem(placed) }
            .onClick { click ->
                if (!click.requireAlive(placed)) return@onClick
                placed.enabled = !placed.enabled
                PlacedBlockManager.saveNow(placed)
                click.update()
            }

        render.layoutSlot('S')
            .renderWith { storageItem(placed) }
            .onClick { click ->
                if (!click.requireAlive(placed)) return@onClick
                click.openForPlayer(HopperStorageView::class.java, placed)
            }

        render.layoutSlot('U')
            .renderWith { upgradesItem() }
            .onClick { click ->
                if (!click.requireAlive(placed)) return@onClick
                click.openForPlayer(HopperUpgradesView::class.java, placed)
            }
    }

    private fun statusItem(placed: PlacedBlock): ItemStack {
        val capacity = VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))
        val radius = VacuumHopperUpgrade.radiusFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Radius)).toInt()
        val interval = VacuumHopperUpgrade.tickIntervalFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Speed))

        return itemStack(Material.CONDUIT) {
            meta {
                name("<white>Coletor de Itens")
                setLore {
                    +"<gray>Recolhe todos os recursos em"
                    +"<gray>um raio de <white>$radius <gray>blocos."
                    +""
                    +" <dark_gray>▸ <white>Armazenamento: <aqua>${placed.storageItems().size}/$capacity"
                    +" <dark_gray>▸ <white>Cooldown: <aqua>a cada ${NumberFormatter.formatNumber(interval / 20.0)}s"
                    +""
                    +"  <white>Status: <gray>${if (placed.enabled) "<green>■ Ligado" else "<red>■ Desligado"}"
                    +""
                }
            }
        }
    }

    private fun toggleItem(placed: PlacedBlock) =
        itemStack(Material.PAPER) {
            meta {
                itemModel = NamespacedKey("oneblock", if (placed.enabled) "on" else "off")
                name(if (placed.enabled) "<green>Ligado" else "<red>Desligado")
                setLore {
                    +"<white><glyph:mouse_right> <gray>Clique <white>para ${if (placed.enabled) "desligar" else "ligar"}."
                }
            }
        }

    private fun storageItem(placed: PlacedBlock): ItemStack {
        val capacity = VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))
        return itemStack(Material.CHEST) {
            meta {
                name("<white>Armazenamento")
                setLore {
                    +"<gray>Itens coletados: <white>${placed.storageItems().size}/$capacity"
                    +""
                    +"<white><glyph:mouse_right> <gray>Clique <white>para acessar."
                }
            }
        }
    }

    private fun upgradesItem() = itemStack(Material.NETHER_STAR) {
        meta {
            name("<white>Upgrades")
            setLore {
                +"<gray>Evolua raio, armazenamento"
                +"<gray>e velocidade."
                +""
                +"<white><glyph:mouse_right> <gray>Clique <white>para acessar."

            }
        }
    }
}
