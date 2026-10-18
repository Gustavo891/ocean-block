package one_block.system.minions.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import me.devnatan.inventoryframework.state.StateValueHost
import one_block.libs.formatter.Mini
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.Items.createEmptyRender
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.currencies.Currencies
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class UpgradesView : View() {

    private val minionState = initialState<PlacedMinion>()
    private val pageState = mutableState(0)

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:minion_upgrade>")
        )
        config.layout(
            " 1 2 3 4 ",
            "         ",
            "   pvn   ",
        )
    }

    override fun onOpen(open: me.devnatan.inventoryframework.context.OpenContext) {
        open.modifyConfig().title(
            Mini.mm.deserialize(
                "<white><shift:-8><glyph:minion_upgrade>"
            )
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placedMinion = minionState.get(render)
        val upgrades = MinionUpgrade.valuesFor(placedMinion.type)
        val pages = pageCount(upgrades.size)

        render.createEmptyRender('v') {
            openForPlayer(MinionView::class.java, placedMinion)
        }

        renderPageArrow(render, 'p', pages, step = -1)
        renderPageArrow(render, 'n', pages, step = 1)

        for (index in 0 until UPGRADES_PER_PAGE) {
            render.layoutSlot('1' + index)
                .watch(pageState)
                .renderWith { upgradeRender(render, placedMinion, upgrades, index) }
                .onClick { click -> buy(click, upgradeAt(click, upgrades, index) ?: return@onClick) }
        }
    }

    private fun upgradeRender(
        render: RenderContext,
        placedMinion: PlacedMinion,
        upgrades: List<MinionUpgrade>,
        index: Int,
    ): ItemStack {
        val upgrade = upgradeAt(render, upgrades, index) ?: return blankItem()
        return upgradeItem(render.player, placedMinion, upgrade)
    }

    private fun upgradeAt(
        host: StateValueHost,
        upgrades: List<MinionUpgrade>,
        index: Int,
    ): MinionUpgrade? = upgrades.getOrNull(pageState.get(host) * UPGRADES_PER_PAGE + index)

    private fun pageCount(total: Int): Int =
        ((total + UPGRADES_PER_PAGE - 1) / UPGRADES_PER_PAGE).coerceAtLeast(1)

    private fun renderPageArrow(render: RenderContext, slot: Char, pages: Int, step: Int) {
        if (pages <= 1) {
            render.layoutSlot(slot).renderWith { blankItem() }
            return
        }

        render.layoutSlot(slot)
            .watch(pageState)
            .renderWith { pageArrowItem(render, pages, step) }
            .onClick { click -> turnPage(click, pages, step) }
    }

    private fun pageArrowItem(render: RenderContext, pages: Int, step: Int) = itemStack(Material.PAPER) {
        val target = pageState.get(render) + step
        val available = target in 0 until pages

        meta {
            if (!available) {
                isHideTooltip = true
                itemModel = NamespacedKey("oneblock", "blank")
                name("<dark_gray>")
                return@meta
            }

            itemModel = NamespacedKey("oneblock", if (step > 0) "forward" else "backward")
            name(if (step > 0) "<yellow>Próxima página" else "<yellow>Página anterior")
            setLore { +"<gray>Página <white>${target + 1}<gray>/<white>$pages" }
        }
    }

    private fun turnPage(click: SlotClickContext, pages: Int, step: Int) {
        val target = pageState.get(click) + step
        if (target !in 0 until pages) return

        pageState.set(target, click)
        click.player.playSound(click.player, Sound.UI_BUTTON_CLICK, 1f, 1f)
    }

    private fun upgradeItem(
        player: Player,
        placedMinion: PlacedMinion,
        upgrade: MinionUpgrade,
    ) = itemStack(Material.PAPER) {
        val level = placedMinion.getUpgradeLevel(upgrade)
        val atMax = level >= upgrade.maxLevel
        val cost = upgrade.costFor(level)
        val canBuy = !atMax && Currencies.has(player.uniqueId, MinionUpgrade.UPGRADE_CURRENCY, cost)
        val stars = "<gold>${"★".repeat(level)}<dark_gray>${"★".repeat(upgrade.maxLevel - level)}"

        meta {
            itemModel = upgrade.itemModel(level) ?: NamespacedKey("oneblock", "blank")
            name(if (atMax) "<gold>${upgrade.prettyName}" else "<yellow>${upgrade.prettyName}")
            setLore {
                +"$stars <dark_gray>($level/${upgrade.maxLevel})"
                +""
                upgrade.lore.forEach { +"<color:#b4b4b8>$it" }
                +""
                +" <dark_gray>▪ <white>Atual: ${upgrade.getStatLine(level, upgrade.maxLevel)}"
                if (atMax) {
                    +" <dark_gray>▪ <white>Nível: <red>Máx"
                    return@setLore
                }

                +" <dark_gray>▪ <white>Próximo: ${upgrade.getStatLine(level + 1, upgrade.maxLevel)}"
                +" <dark_gray>▪ <white>Custo: <dark_green>$<white>${NumberFormatter.format(cost)}"
                +""
                +(if (canBuy) "<green>Clique para evoluir!" else "<red>Dinheiro insuficiente!")
            }
        }
    }

    private fun blankItem() = itemStack(Material.PAPER) {
        meta {
            itemModel = NamespacedKey("oneblock", "blank")
            isHideTooltip = true
        }
    }

    private fun buy(click: SlotClickContext, upgrade: MinionUpgrade) {
        val placedMinion = minionState.get(click)
        val player = click.player
        val level = placedMinion.getUpgradeLevel(upgrade)

        if (level >= upgrade.maxLevel) {
            player.sendRichMessage("<red>Essa melhoria já está no nível máximo!")
            return
        }

        val cost = upgrade.costFor(level)
        if (!Currencies.has(player.uniqueId, MinionUpgrade.UPGRADE_CURRENCY, cost)) {
            player.sendRichMessage("<red>Você precisa de <white>${NumberFormatter.format(cost)} coins<red> para evoluir.")
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        Currencies.withdraw(player.uniqueId, MinionUpgrade.UPGRADE_CURRENCY, cost)
        placedMinion.setUpgradeLevel(upgrade, level + 1)
        PlacedMinionManager.update(placedMinion)

        player.sendRichMessage("<green>${upgrade.prettyName} evoluído para o nível <white>${level + 1}<green>!")
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)

        click.update()
    }

    private companion object {
        const val UPGRADES_PER_PAGE = 4
    }
}
