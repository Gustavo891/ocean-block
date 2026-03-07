package one_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.item_builder.addLore
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.system.currencies.Currencies
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandUpgrade

class UpgradeView : View() {

    private val islandState = initialState<Island>()

    private val pagination = lazyPaginationState(
        { ctx -> IslandUpgrade.entries.toMutableList() }
    ) { ctx, builder, index, value ->
        builder.renderWith { createUpgradeItem(ctx, value) }
            .onClick { ctx -> handleUpgradeClick(ctx, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "         ",
            " O O O O ",
            "         "
        )
    }

    private fun createUpgradeItem(ctx: Context, value: IslandUpgrade) = itemStack(value.iconMaterial) {
        val size = islandState.get(ctx).getUpgradeLevel(IslandUpgrade.Size)
        val isMaxLevel = size == IslandUpgrade.Size.maxLevel

        meta {
            name(value.prettyName)

            addLore {
                +"<gray>Nível: ${size}/${value.maxLevel}"
                +"<gray>Preço: ${value.getPrice(size)}"

                if (isMaxLevel) {
                    +"<red>Nível máximo!"
                } else {
                    +"<green>Clique para upar!"
                }
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