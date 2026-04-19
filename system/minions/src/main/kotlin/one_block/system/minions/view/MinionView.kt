package one_block.system.minions.view

import com.destroystokyo.paper.profile.ProfileProperty
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.minions.Minions
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.system.minions.view.UpgradesView
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class MinionView : View() {

    private val minionState = initialState<PlacedMinion>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            " TE P UX "
        )
    }

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title(MiniMessage.miniMessage().deserialize("<shift:-8><glyph:minion_menu><font:oneblock:gui><shift:-170><dark_gray>${minionState.get(open).type.prettyName}"))
    }

    override fun onClick(click: SlotClickContext) {
        val slot = click.slot
        if (slot >= 27) {
            val minion = minionState.get(click)
            val item = click.item ?: return
            val player = click.player
            if (minion.getTool() == null) {
                if (minion.type.acceptedToolMaterials.contains(item.type)) {
                    minion.setTool(item.clone())
                    player.inventory.remove(item)
                    PlacedMinionManager.update(minion)
                    PlacedMinionManager.updateArmorStandHand(minion)
                    click.parent.update()
                }
            }

        }
    }

    override fun onFirstRender(render: RenderContext) {
        val placedMinion = minionState.get(render)
        val player = render.player


        render.layoutSlot('T')
            .renderWith { toggleItem(placedMinion.enabled) }
            .onClick { _ ->
                placedMinion.enabled = !placedMinion.enabled
                PlacedMinionManager.update(placedMinion)
                render.update()
            }


        render.layoutSlot('E')
            .renderWith { batteryItem(placedMinion) }

        render.layoutSlot('U')
            .renderWith { upgradesItem() }
            .onClick { click ->
                if (MinionUpgrade.valuesFor(placedMinion.type).isNotEmpty()) {
                    click.openForPlayer(UpgradesView::class.java, placedMinion)
                }
            }

        render.layoutSlot('P')
            .renderWith { toolItem(placedMinion) }
            .onClick { click ->
                val player = click.player
                val current = placedMinion.getTool()

                if(current != null) {
                    player.inventory.addItem(current).values
                        .forEach { player.world.dropItemNaturally(player.location, it) }
                    placedMinion.setTool(null)
                    PlacedMinionManager.update(placedMinion)
                    PlacedMinionManager.updateArmorStandHand(placedMinion)

                }

                render.update()
            }

        render.layoutSlot('X')
            .renderWith { removeItem() }
            .onClick { _ ->
                render.closeForPlayer()

                val location = placedMinion.location.toBukkit()
                location?.world?.getNearbyEntitiesByType(ArmorStand::class.java, location, 0.5, 1.0)
                    ?.firstOrNull { Minions.getMinionId(it) == placedMinion.uuid }
                    ?.remove()

                PlacedMinionManager.unregister(placedMinion.location)

                val minionItem = Minions.createMinionItem(placedMinion)
                val leftovers = player.inventory.addItem(minionItem)
                leftovers.values.forEach { player.world.dropItemNaturally(player.location, it) }

                player.sendMessage("§cMinion §f${placedMinion.type.prettyName} §cfoi recolhido com sucesso!")
            }
    }

    private fun minionItem(placedMinion: PlacedMinion) = itemStack(Material.PLAYER_HEAD) {
        meta<SkullMeta>{
            playerProfile = Bukkit.createProfile(UUID.randomUUID(), "1").apply {
                setProperty(
                    ProfileProperty(
                        "textures",
                        placedMinion.type.headValue
                    )
                )
                itemModel = NamespacedKey("oneblock", "head_minion")
            }
        }
    }

    private fun batteryItem(placedMinion: PlacedMinion): org.bukkit.inventory.ItemStack {
        val energy = placedMinion.energy
        val max = PlacedMinion.MAX_ENERGY
        val pct = energy.toDouble() / max

        val filled = (pct * 10).toInt().coerceIn(0, 10)
        val bar = "<green>${"■".repeat(filled)}<dark_gray>${"■".repeat(10 - filled)}"

        return itemStack(Material.PAPER) {
            meta {
                itemModel = NamespacedKey("oneblock", "battery1")
                name("<yellow>Bateria")
                setLore {
                    +"<gray>Fonte de energia para"
                    +"<gray>seu minion funcionar."
                    +""
                    +"  <dark_gray>▸ <white>Energia: <aqua>${NumberFormatter.format(energy.toDouble())}/${
                        NumberFormatter.format(
                            max.toDouble()
                        )
                    }"
                    +"    $bar <gray>${(pct * 100).toInt()}%"
                    +""
                }
            }
        }
    }

    private fun upgradesItem() = itemStack(Material.END_CRYSTAL) {
        meta {
            name("<yellow>Melhorias")
            setLore {
                +"<gray>Clique para acessar as"
                +"<gray>melhorias do seu minion."
            }
        }
    }

    private fun toggleItem(enabled: Boolean) = if (enabled) {
        itemStack(Material.PAPER) {
            meta {
                itemModel = NamespacedKey("oneblock", "on")
                name("<green>Ativado")
                setLore {
                    +"<gray>Clique para pausar."
                }
            }
        }
    } else {
        itemStack(Material.PAPER) {
            meta {
                itemModel = NamespacedKey("oneblock", "off")
                name("<red>Desativado")
                setLore {
                    +"<gray>Clique para ativar."
                }
            }
        }
    }

    private fun toolItem(placedMinion: PlacedMinion): org.bukkit.inventory.ItemStack {
        val current = placedMinion.getTool()
        if (current != null) return current

        val toolName = placedMinion.type.toolName
        val placeholder = Material.BARRIER
        return itemStack(placeholder) {
            meta {
                name("<red>Sem ferramenta")
                setLore {
                    +"<gray>Clique em uma ferramenta em seu"
                    +"<gray>inventário para adiciona-la."
                }
            }
        }
    }

    private fun removeItem() = itemStack(Material.PAPER) {
        meta {
            itemModel = NamespacedKey("oneblock", "deny")
            name("<red>Remover")
            setLore {
                +"<gray>Clique para recolher"
                +"<gray>esse minion."

            }
        }
    }
}
