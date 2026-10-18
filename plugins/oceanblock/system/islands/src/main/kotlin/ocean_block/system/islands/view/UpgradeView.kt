package ocean_block.system.islands.view

import com.destroystokyo.paper.profile.ProfileProperty
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.item_builder.Items.createEmptyRender
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.currencies.Currencies
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.islands.island.IslandUpgrade
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class UpgradeView : View() {

    private val islandState = initialState<Island>()

    private val pagination = lazyPaginationState(
        { ctx -> IslandUpgrade.entries.toMutableList() }
    ) { ctx, builder, index, value ->
        builder.renderWith { createUpgradeItem(ctx, value, false) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "O77O88O99",
            "777888999",
            "777888999",
            "414525636",
            "    v    "
        )
        config.title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:island_upgrades><font:oneblock:gui><shift:-170><gray>Ilha > <dark_gray>Melhorias")
        )
    }



    override fun onFirstRender(render: RenderContext) {

        val pagination = pagination.get(render)
        val player = render.player
        val currentUpgrades = IslandUpgrade.getPage(pagination.currentPageIndex())

        render.createEmptyRender('v') {
            render.openForPlayer(IslandView::class.java)
        }

        currentUpgrades.forEachIndexed { index, upgrade ->
            render.layoutSlot('7' + index)
                .renderWith { createUpgradeItem(render, upgrade, true) }
            render.layoutSlot('1' + index)
                .renderWith { createBuyItem(render.player, upgrade, islandState.get(render), false) }
                .onClick { click ->
                    handleUpgradeClick(click, upgrade)
                }
            render.layoutSlot('4' + index)
                .renderWith { createBuyItem(render.player, upgrade, islandState.get(render), true) }
                .onClick { click ->
                    handleUpgradeClick(click, upgrade)
                }
        }


    }

    private fun createBuyItem(player: Player, upgrade: IslandUpgrade, island: Island, blank: Boolean) = itemStack(Material.PAPER) {

        val level = island.getUpgradeLevel(upgrade)
        val isBuyable: Boolean = run {
            level < upgrade.maxLevel && Currencies.has(player.uniqueId, "coins", upgrade.getPrice(level))
        }

        meta {
            itemModel = NamespacedKey("oneblock", if (blank) "blank" else if(isBuyable) "buy_button" else if(level >= upgrade.maxLevel) "max_button" else "deny_button")
            if(level >= upgrade.maxLevel) {
                name("<red>Limite atingido!")
            } else if (!isBuyable) {
                name("<red>Dinheiro insuficiente!")
                addLore {
                    +"<white>Necessário: <dark_green>$<white>${NumberFormatter.format(upgrade.getPrice(level))}"
                }
            } else {
                name("<green>Confirmar!")
                addLore {
                    +"<white>Custo: <dark_green>$<white>${NumberFormatter.format(upgrade.getPrice(level))}"
                }
            }
        }
    }

    private fun createUpgradeItem(ctx: Context, value: IslandUpgrade, blank: Boolean) = itemStack(Material.PLAYER_HEAD) {
        val island = islandState.get(ctx)
        val size = island.getUpgradeLevel(value)
        val isMaxLevel = size == value.maxLevel

        meta<SkullMeta> {
            itemModel = NamespacedKey("oneblock", if(blank) "blank" else "head2d")
            name("<white>${value.prettyName}")
            addLore {
                value.lore.forEach { + "<color:#b4b4b8>$it" }
                +""
                +" <dark_gray>▪ <white>Nível: ${if(isMaxLevel) "<red>Máx" else "<green>${island.getMaxMembers(size)} <dark_gray>➡ <green>${island.getMaxMembers(size + 1)}"}"
                +" <dark_gray>▪ <white>Preço: <dark_green>$<white>${NumberFormatter.format(value.getPrice(size))}"
                +""
            }
            playerProfile = Bukkit.createProfile(UUID.randomUUID(), "1").apply {
                setProperty(
                    ProfileProperty(
                        "textures",
                        value.texture
                    )
                )
            }
        }

    }

    private fun handleUpgradeClick(ctx: SlotClickContext, value: IslandUpgrade) {
        val island = islandState.get(ctx)
        val player = ctx.player

        val size = island.getUpgradeLevel(IslandUpgrade.Size)
        if (size == IslandUpgrade.Size.maxLevel) {
            player.sendRichMessage("<red>Você já chegou ao nível máximo.")
            return
        }

        val price = IslandUpgrade.Size.getPrice(size)
        if (!Currencies.has(player.uniqueId, "coins", price)) {
            player.sendRichMessage("<red>Você não possui dinheiro suficiente para upar!")
            return
        }

        Currencies.withdraw(player.uniqueId, "coins", price)
        value.execute(player, island)

        IslandManager.markDirty(island.uuid)
        pagination.get(ctx).forceUpdate()
    }
}
