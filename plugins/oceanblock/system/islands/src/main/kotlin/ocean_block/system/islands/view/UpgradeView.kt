package ocean_block.system.islands.view

import com.destroystokyo.paper.profile.ProfileProperty
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.item_builder.addLore
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.system.currencies.Currencies
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.islands.island.UpgradePurchase
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class UpgradeView : View() {

    private val islandState = initialState<Island>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(menuTitle("Melhorias"))
        config.layout(
            "         ",
            "         ",
            "    v    "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val island = islandState.get(render)

        render.layoutSlot('v')
            .renderWith { backItem() }
            .onClick { click -> click.openForPlayer(IslandView::class.java, island) }

        IslandUpgrade.entries.forEachIndexed { index, upgrade ->
            render.slot(2, 2 + index * 2)
                .renderWith { upgradeItem(render.player, island, upgrade) }
                .onClick { click -> buy(click, island, upgrade) }
        }
    }

    private fun upgradeItem(player: Player, island: Island, upgrade: IslandUpgrade) = itemStack(Material.PLAYER_HEAD) {
        val level = island.getUpgradeLevel(upgrade)
        val atMax = level >= upgrade.maxLevel
        val price = upgrade.getPrice(level)

        meta<SkullMeta> {
            name("<yellow>${upgrade.prettyName}")
            addItemFlags(*ItemFlag.entries.toTypedArray())
            addLore {
                upgrade.lore.forEach { +"<gray>$it" }
                +""
                +"<white>Nível: <green>$level<dark_gray>/<gray>${upgrade.maxLevel}"
                if (!atMax) +"<white>Próximo nível: <dark_green>$<white>${NumberFormatter.format(price)}"
                +""
                +when {
                    atMax -> "<green>Nível máximo!"
                    Currencies.has(player.uniqueId, "coins", price) -> "<yellow>Clique para melhorar!"
                    else -> "<red>Coins insuficientes."
                }
            }
            playerProfile = Bukkit.createProfile(UUID.randomUUID(), "1").apply {
                setProperty(ProfileProperty("textures", upgrade.texture))
            }
        }
    }

    private fun buy(click: SlotClickContext, island: Island, upgrade: IslandUpgrade) {
        if (UpgradePurchase.buy(click.player, island, upgrade)) click.update()
    }
}
