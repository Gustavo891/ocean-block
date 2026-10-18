package one_block.system.custom_items.content.vacuum_hopper.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.formatter.Mini
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.Items.createEmptyRender
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.currencies.Currencies
import one_block.system.custom_items.block.BlockUpgrade
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopper
import one_block.system.custom_items.content.vacuum_hopper.VacuumHopperUpgrade
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player

class HopperUpgradesView : View() {

    private val blockState = initialState<PlacedBlock>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:coletor_upgrades>")
        )
        config.layout(
            "111222333",
            "111222333",
            "111222333",
            "AaABbBCcC",
            "    v    ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placed = blockState.get(render)

        render.createEmptyRender('v') {
            if (!requireAlive(placed)) return@createEmptyRender
            openForPlayer(VacuumHopperView::class.java, placed)
        }

        VacuumHopperUpgrade.entries.forEachIndexed { index, upgrade ->
            render.layoutSlot('1' + index)
                .renderWith { upgradeItem(placed, upgrade) }

            render.layoutSlot('a' + index)
                .renderWith { buyItem(render.player, placed, upgrade, false) }
                .onClick { click -> buy(click, upgrade) }

            render.layoutSlot('A' + index)
                .renderWith { buyItem(render.player, placed, upgrade, true) }
                .onClick { click -> buy(click, upgrade) }
        }
    }

    private fun upgradeItem(placed: PlacedBlock, upgrade: BlockUpgrade) = itemStack(Material.PAPER) {
        val level = placed.getUpgradeLevel(upgrade)
        val atMax = level >= upgrade.maxLevel
        val stars = "<gold>${"★".repeat(level)}<dark_gray>${"★".repeat(upgrade.maxLevel - level)}"

        meta {
            itemModel = NamespacedKey("oneblock", "blank")
            name(if (atMax) "<gold>${upgrade.prettyName}" else "<yellow>${upgrade.prettyName}")
            setLore {
                +"$stars <dark_gray>($level/${upgrade.maxLevel})"
                +""
                upgrade.lore.forEach { +"<color:#b4b4b8>$it" }
                +""
                +" <dark_gray>▪ <white>Atual: ${upgrade.statLine(level)}"
                if (!atMax) {
                    +" <dark_gray>▪ <white>Próximo: ${upgrade.statLine(level + 1)}"
                    +" <dark_gray>▪ <white>Custo: <dark_green>$<white>${NumberFormatter.format(upgrade.costFor(level))}"
                } else {
                    +" <dark_gray>▪ <white>Nível: <red>Máx"
                }
            }
        }
    }

    private fun buyItem(player: Player, placed: PlacedBlock, upgrade: BlockUpgrade, blank: Boolean) = itemStack(Material.PAPER) {
        val level = placed.getUpgradeLevel(upgrade)
        val atMax = level >= upgrade.maxLevel
        val cost = upgrade.costFor(level)
        val canBuy = !atMax && Currencies.has(player.uniqueId, VacuumHopperUpgrade.UPGRADE_CURRENCY, cost)

        meta {
            itemModel = NamespacedKey(
                "oneblock",
                when {
                    blank -> "blank"
                    atMax -> "max_button"
                    canBuy -> "buy_button"
                    else -> "deny_button"
                }
            )
            when {
                atMax -> name("<red>Limite atingido!")
                canBuy -> {
                    name("<green>Confirmar!")
                    setLore { +"<white>Custo: <dark_green>$<white>${NumberFormatter.format(cost)}" }
                }
                else -> {
                    name("<red>Dinheiro insuficiente!")
                    setLore { +"<white>Necessário: <dark_green>$<white>${NumberFormatter.format(cost)}" }
                }
            }
        }
    }

    private fun buy(click: SlotClickContext, upgrade: BlockUpgrade) {
        val placed = blockState.get(click)
        if (!click.requireAlive(placed)) return

        val player = click.player
        val level = placed.getUpgradeLevel(upgrade)

        if (level >= upgrade.maxLevel) {
            player.sendRichMessage("<red>Esse upgrade já está no nível máximo!")
            return
        }

        val cost = upgrade.costFor(level)
        if (!Currencies.has(player.uniqueId, VacuumHopperUpgrade.UPGRADE_CURRENCY, cost)) {
            player.sendRichMessage("<red>Você precisa de <white>${NumberFormatter.format(cost)} coins<red> para evoluir.")
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        Currencies.withdraw(player.uniqueId, VacuumHopperUpgrade.UPGRADE_CURRENCY, cost)
        placed.setUpgradeLevel(upgrade, level + 1)
        if (upgrade == VacuumHopperUpgrade.Storage) VacuumHopper.clearFullNotice(placed)
        PlacedBlockManager.saveNow(placed)

        player.sendRichMessage("<green>${upgrade.prettyName} evoluído para o nível <white>${level + 1}<green>!")
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)

        click.update()
    }
}
