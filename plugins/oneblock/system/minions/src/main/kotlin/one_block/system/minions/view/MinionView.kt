package one_block.system.minions.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.formatter.Mini
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.getNbtBoolean
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.nbt
import one_block.libs.item_builder.setLore
import one_block.system.minions.Minions
import one_block.system.minions.enums.FullMode
import one_block.system.minions.enums.MinionType
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import java.text.SimpleDateFormat
import java.util.Date

class MinionView : View() {

    private val minionState = initialState<PlacedMinion>()

    private val pagination = lazyPaginationState(
        { ctx -> gridItems(minionState.get(ctx)) }
    ) { _, builder, _, value ->
        builder.renderWith { value }
            .onClick { click ->
                if (value.getNbtBoolean(XP_ITEM_KEY)) collectXp(click) else withdraw(click, value)
            }
    }

    private fun gridItems(minion: PlacedMinion): MutableList<ItemStack> {
        val items = minion.storageItems().toMutableList()
        if (minion.type == MinionType.Terminator && minion.xpAmount > 0) {
            items.add(0, xpItem(minion.xpAmount))
        }
        return items
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:minion_menu>")
        )
        config.layout(
            " I OOOOO ",
            " T OOOOO ",
            " E OOOOO ",
            " U OOOOO ",
            " X VLSMP ",
        )
    }

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:minion_menu><font:oneblock:gui><shift:-170><gray>Minion > <dark_gray>${minionState.get(open).type.prettyName}")
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placedMinion = minionState.get(render)
        val player = render.player

        render.layoutSlot('I')
            .renderWith { renderIcon(placedMinion) }

        render.layoutSlot('T')
            .renderWith { toggleItem(placedMinion.enabled) }
            .onClick { _ ->
                placedMinion.enabled = !placedMinion.enabled
                PlacedMinionManager.update(placedMinion)
                render.update()
            }

        render.layoutSlot('E')
            .renderWith { batteryItem(placedMinion) }

        render.layoutSlot('S')
            .renderWith { collectAllItem(placedMinion) }
            .onClick { click -> collectAll(click) }

        render.layoutSlot('L')
            .renderWith { filterItem(placedMinion) }
            .onClick { click -> click.openForPlayer(FilterView::class.java, placedMinion) }

        render.layoutSlot('M')
            .renderWith { fullModeItem(placedMinion) }
            .onClick { click -> cycleFullMode(click) }

        render.layoutSlot('V')
            .renderWith { pageItem(pagination.get(render), forward = false) }
            .onClick { click ->
                val page = pagination.get(click)
                if (!page.canBack()) return@onClick
                page.back()
                click.update()
            }

        render.layoutSlot('P')
            .renderWith { pageItem(pagination.get(render), forward = true) }
            .onClick { click ->
                val page = pagination.get(click)
                if (!page.canAdvance()) return@onClick
                page.advance()
                click.update()
            }

        render.layoutSlot('U')
            .renderWith { upgradesItem() }
            .onClick { click ->
                if (MinionUpgrade.valuesFor(placedMinion.type).isNotEmpty()) {
                    click.openForPlayer(UpgradesView::class.java, placedMinion)
                }
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

    private fun renderIcon(placedMinion: PlacedMinion): ItemStack {
        val upgrades = MinionUpgrade.valuesFor(placedMinion.type)
        val storageUsed = placedMinion.storageItems().size
        val storageTotal = placedMinion.storageCapacity()

        return itemStack(Material.PAPER) {
            meta {
                placedMinion.type.itemModel?.let { model -> itemModel = model }
                name("<white>${placedMinion.type.prettyName}")
                setLore {
                    placedMinion.type.desc.forEach { +"<gray>$it" }
                    +""
                    +"  <dark_gray>▸ <white>Dono: <aqua>${ownerName(placedMinion)}"
                    +"  <dark_gray>▸ <white>Colocado: <aqua>${placedAtLabel(placedMinion)}"
                    +""
                    +"  <dark_gray>▸ <white>${placedMinion.type.producedLabel}: <aqua>${count(placedMinion.producedCount)}"
                    +"  <dark_gray>▸ <white>Energia gasta: <aqua>${count(placedMinion.energySpentTotal)}"
                    +"  <dark_gray>▸ <white>Armazém: <aqua>$storageUsed<gray>/<aqua>$storageTotal <gray>slots"
                    if (placedMinion.xpAmount > 0) {
                        +"  <dark_gray>▸ <white>XP guardado: <aqua>${count(placedMinion.xpAmount)}"
                    }

                    if (upgrades.isNotEmpty()) {
                        +""
                        +"<white>Melhorias:"
                        upgrades.forEach { upgrade ->
                            val level = placedMinion.getUpgradeLevel(upgrade)
                            val color = if (level >= upgrade.maxLevel) "<green>" else "<aqua>"
                            +"  <dark_gray>▸ <white>${upgrade.prettyName}: $color$level<gray>/<white>${upgrade.maxLevel}"
                        }
                    }
                }
            }
        }
    }

    private fun ownerName(placedMinion: PlacedMinion): String {
        val uuid = placedMinion.ownerUuid ?: return "Desconhecido"
        return Bukkit.getOfflinePlayer(uuid).name ?: "Desconhecido"
    }

    private fun placedAtLabel(placedMinion: PlacedMinion): String {
        val millis = placedMinion.placedAt ?: return "Desconhecido"
        return DATE_FORMAT.format(Date(millis))
    }

    private fun count(value: Int): String = NumberFormatter.format(value.toDouble())

    private fun batteryItem(placedMinion: PlacedMinion): ItemStack {
        val energy = placedMinion.energy
        val max = placedMinion.getMaxEnergy()
        val pct = energy.toDouble() / max

        val filled = (pct * 10).toInt().coerceIn(0, 10)
        val bar = "<green>${"■".repeat(filled)}<dark_gray>${"■".repeat(10 - filled)}"

        return itemStack(Material.REDSTONE) {
            meta {

                itemModel = NamespacedKey("oneblock", "bateria")
                name("<white>Bateria")
                setLore {
                    +"<gray>Fonte de energia para"
                    +"<gray>seu minion funcionar."
                    +""
                    +"  <dark_gray>▸ <white>Energia: <aqua>${NumberFormatter.format(energy.toDouble())}/${
                        NumberFormatter.format(max.toDouble())
                    }"
                    +"    $bar <gray>${(pct * 100).toInt()}%"
                    +""
                }
            }
        }
    }

    private fun xpItem(amount: Int) = itemStack(Material.EXPERIENCE_BOTTLE) {
        meta {
            name("<green>${NumberFormatter.format(amount.toDouble())} de XP")
            setLore {
                +"<gray>Experiência das mortes"
                +"<gray>feitas por esse minion."
                +""
                +"<yellow>Clique para coletar."
            }
            nbt { boolean(XP_ITEM_KEY, true) }
        }
    }

    private fun collectXp(click: SlotClickContext) {
        val minion = minionState.get(click)
        val xp = minion.takeXp()
        if (xp <= 0) return

        click.player.giveExp(xp)
        click.player.sendRichMessage("<green>+$xp de XP.")

        PlacedMinionManager.update(minion)
        pagination.get(click).forceUpdate()
        click.update()
    }

    private fun withdraw(click: SlotClickContext, value: ItemStack) {
        val minion = minionState.get(click)
        val stored = minion.storageItems().toMutableList()

        val index = stored.indexOfFirst { it.isSimilar(value) && it.amount == value.amount }
        if (index == -1) return

        val leftover = click.player.inventory.addItem(value.clone())
        if (leftover.isEmpty()) {
            stored.removeAt(index)
        } else {
            stored[index] = leftover.values.first()
        }

        minion.setStorage(stored)
        PlacedMinionManager.update(minion)
        pagination.get(click).forceUpdate()
        click.update()
    }

    private fun collectAll(click: SlotClickContext) {
        val minion = minionState.get(click)
        val remaining = mutableListOf<ItemStack>()

        minion.storageItems().forEach { item ->
            val leftover = click.player.inventory.addItem(item.clone())
            remaining += leftover.values
        }

        minion.setStorage(remaining)

        val xp = minion.takeXp()
        if (xp > 0) {
            click.player.giveExp(xp)
            click.player.sendRichMessage("<green>+$xp de XP.")
        }

        PlacedMinionManager.update(minion)
        pagination.get(click).forceUpdate()
        click.update()

        if (remaining.isEmpty()) {
            click.player.sendRichMessage("<green>Itens coletados.")
        } else {
            click.player.sendRichMessage("<yellow>Seu inventário encheu; parte dos itens ficou no minion.")
        }
    }

    private fun cycleFullMode(click: SlotClickContext) {
        val minion = minionState.get(click)
        minion.fullMode = minion.fullMode.next()
        PlacedMinionManager.update(minion)
        click.update()
    }

    private fun collectAllItem(minion: PlacedMinion) = itemStack(Material.HOPPER) {
        meta {
            name("<gold>Armazém")
            setLore {
                +"<gray>Itens coletados pelo minion."
                +""
                +"  <dark_gray>▸ <white>Uso: <aqua>${minion.storageItems().size}<gray>/<aqua>${minion.storageCapacity()} <gray>slots"
                if (minion.xpAmount > 0) {
                    +"  <dark_gray>▸ <white>XP guardado: <aqua>${minion.xpAmount}"
                }
                +""
                +"<yellow>Clique para coletar tudo."
            }
        }
    }

    private fun filterItem(minion: PlacedMinion) = itemStack(Material.COMPARATOR) {
        meta {
            name("<gold>Filtro")
            setLore {
                +"<gray>Controla quais itens o"
                +"<gray>minion guarda."
                +""
                +"  <dark_gray>▸ <white>Modo: <aqua>${minion.filterMode.prettyName}"
                +"  <dark_gray>▸ <white>Itens: <aqua>${minion.filter.size}"
                +""
                +"<yellow>Clique para abrir."
            }
        }
    }

    private fun fullModeItem(minion: PlacedMinion): ItemStack {
        val mode = minion.fullMode
        val material = when (mode) {
            FullMode.STOP -> Material.REDSTONE_TORCH
            FullMode.DROP -> Material.DROPPER
            FullMode.TRASH -> Material.LAVA_BUCKET
        }
        return itemStack(material) {
            meta {
                name("<yellow>Quando cheio: <white>${mode.prettyName}")
                setLore {
                    +"<gray>${mode.description}"
                    +""
                    FullMode.entries.forEach { option ->
                        if (option == mode) {
                            +"<green>▸ <white>${option.prettyName}"
                        } else {
                            +"<dark_gray>  ${option.prettyName}"
                        }
                    }
                    +""
                    +"<yellow>Clique para alternar."
                }
            }
        }
    }

    private fun pageItem(page: Pagination, forward: Boolean): ItemStack {
        val enabled = if (forward) page.canAdvance() else page.canBack()

        return itemStack(Material.PAPER) {
            meta {
                if (!enabled) {
                    isHideTooltip = true
                    itemModel = NamespacedKey("oneblock", "blank")
                    name("<dark_gray>")
                    return@meta
                }

                itemModel = NamespacedKey("oneblock", if (forward) "forward" else "backward")
                name(if (forward) "<yellow>Próxima página" else "<yellow>Página anterior")
                setLore {
                    +"<gray>Página <white>${page.currentPage()}<gray>/<white>${page.lastPage()}"
                }
            }
        }
    }

    private fun upgradesItem() = itemStack(Material.PAPER) {
        meta {
            itemModel = NamespacedKey("oneblock", "hammer")
            name("<yellow>Melhorias")
            setLore {
                +"<gray>Clique para acessar as"
                +"<gray>melhorias do seu minion."
            }
        }
    }

    private fun toggleItem(status: Boolean) =
        itemStack(Material.PAPER) {
            meta {
                itemModel = NamespacedKey("oneblock", if (status) "on" else "off")
                name(if (status) "<green>Ligado" else "<red>Desligado")
                setLore {
                    +"<white><glyph:mouse_right> <gray>Clique <white>para ${if (status) "desligar" else "ligar"}."
                }
            }
            NumberFormatter.format(100.0)
        }


    private fun removeItem() = itemStack(Material.BARRIER) {
        meta {
            itemModel = NamespacedKey("oneblock", "blank")
            name("<white>Remover")
            setLore {
                +"<gray>Clique para recolher"
                +"<gray>esse minion."
            }
        }
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm")
        val XP_ITEM_KEY = NamespacedKey("minions", "xp_item")
    }
}
