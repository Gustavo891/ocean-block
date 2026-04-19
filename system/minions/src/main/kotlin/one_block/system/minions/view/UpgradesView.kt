package one_block.system.minions.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Material

class UpgradesView : View() {

    private val minionState = initialState<PlacedMinion>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Upgrades")
        config.layout(
            "         ",
            "B        ",
            "         "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placedMinion = minionState.get(render)
        val upgrades = MinionUpgrade.valuesFor(placedMinion.type)

        render.layoutSlot('B')
            .renderWith {
                itemStack(Material.ARROW) {
                    meta {
                        name("<gray>← Voltar")
                    }
                }
            }
            .onClick { click ->
                click.openForPlayer(MinionView::class.java, placedMinion)
            }

        upgrades.forEachIndexed { index, upgrade ->
            render.slot(1, 2 + index)
                .renderWith {
                    val level = placedMinion.getUpgradeLevel(upgrade)
                    val atMax = level >= upgrade.maxLevel
                    val stars = "<gold>${"★".repeat(level)}<dark_gray>${"★".repeat(upgrade.maxLevel - level)}"

                    itemStack(if (atMax) Material.NETHER_STAR else Material.FEATHER) {
                        meta {
                            name(if (atMax) "<gold>${upgrade.prettyName}" else "<yellow>${upgrade.prettyName}")
                            setLore {
                                +"<dark_gray>──────────────"
                                +"$stars <dark_gray>(${level}/${upgrade.maxLevel})"
                                +""
                                for (line in upgrade.lore) {
                                    +"<dark_gray>$line"
                                }
                                +""
                                if (atMax) {
                                    +"<gold>✦ Nível máximo atingido!"
                                } else {
                                    +"<gray>Clique para evoluir."
                                    +"<gray>Requer: <white>1 Pena"
                                }
                                +"<dark_gray>──────────────"
                            }
                        }
                    }
                }
                .onClick { click ->
                    val player = click.player
                    val level = placedMinion.getUpgradeLevel(upgrade)

                    if (level >= upgrade.maxLevel) {
                        player.sendMessage("§cEsse upgrade já está no nível máximo!")
                        return@onClick
                    }

                    val featherSlot = player.inventory.first(Material.FEATHER)
                    if (featherSlot == -1) {
                        player.sendMessage("§cVocê precisa de §f1 Pena §cpara evoluir!")
                        return@onClick
                    }

                    // Consome a pena
                    val feather = player.inventory.getItem(featherSlot)!!
                    if (feather.amount > 1) feather.amount -= 1
                    else player.inventory.setItem(featherSlot, null)

                    // Aplica upgrade e persiste
                    placedMinion.setUpgradeLevel(upgrade, level + 1)
                    PlacedMinionManager.update(placedMinion)

                    player.sendMessage("§8[§6Minion§8] §e${upgrade.prettyName} §7evoluído para §anível ${level + 1}§7! §a✔")

                    // Reabre para atualizar os itens
                    click.openForPlayer(UpgradesView::class.java, placedMinion)
                }
        }
    }
}
