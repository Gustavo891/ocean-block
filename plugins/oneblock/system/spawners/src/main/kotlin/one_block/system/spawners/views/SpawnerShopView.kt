package one_block.system.spawners.views

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.currencies.Currencies
import one_block.system.spawners.models.SpawnerType
import one_block.system.spawners.SpawnersSystem
import org.bukkit.Material
import org.bukkit.entity.Player

class SpawnerShopView : View() {

    private companion object {
        const val CURRENCY = "coins"
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title("Loja de Spawners")
        config.size(if (SpawnerType.entries.size <= 27) 3 else 6)
    }

    override fun onFirstRender(render: RenderContext) {
        SpawnerType.entries.forEach { type ->
            render.availableSlot()
                .renderWith { offerItem(type) }
                .onClick { click -> buy(click.player, type) }
        }
    }

    private fun buy(player: Player, type: SpawnerType) {
        if (!Currencies.has(player.uniqueId, CURRENCY, type.price)) {
            player.sendRichMessage("<red>Coins insuficientes. <gray>(custa ${type.price.toLong()})")
            return
        }

        if (player.inventory.firstEmpty() == -1) {
            player.sendRichMessage("<red>Seu inventário está cheio.")
            return
        }

        Currencies.withdraw(player.uniqueId, CURRENCY, type.price)
        player.inventory.addItem(SpawnersSystem.build(type))
        player.sendRichMessage("<green>Comprado: <white>${type.name}<green> por <white>${type.price.toLong()} coins<green>.")
    }

    private fun offerItem(type: SpawnerType) = itemStack(Material.SPAWNER) {
        meta {
            name(type.displayName)
            setLore {
                +"<dark_gray>▸ <white>Preço: <gold>${type.price.toLong()} coins"
                +"<dark_gray>▸ <white>Ciclo: <aqua>${type.spawnIntervalTicks / 20}s"
                +"<dark_gray>▸ <white>Limite de mobs: <aqua>${type.maxLiveMobs}"
                +""
                +"<yellow>Clique para comprar."
            }
        }
    }
}
