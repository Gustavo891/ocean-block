package one_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import one_block.libs.formatter.NumberFormatter
import one_block.libs.item_builder.*
import one_block.system.currencies.Currencies
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandUpgrade
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta

class IslandView : View() {

    override fun onOpen(open: OpenContext) {
        open.modifyConfig().title("Island")
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
    }

    override fun onFirstRender(render: RenderContext) {
        val player = render.player
        val island = IslandManager.getIslandByPlayer(player.uniqueId) ?: return

        setupTeleportSlot(render, player, island)
     //   setupBorderUpgradeSlot(render, player, island)
     //   setupPlantingSpeedSlot(render, player, island)
        setupMembersSlot(render, player, island)
    }

    private fun setupMembersSlot(render: RenderContext, player: Player, island: Island) {
        render.slot(2, 3)
            .renderWith {
                itemStack(Material.PLAYER_HEAD) {
                    meta<ItemMeta> {
                        name("<yellow>Membros")
                    }
                }
            }
            .onClick { click ->
                render.openForPlayer(IslandMembersView::class.java)
            }

    }

    private fun setupTeleportSlot(render: RenderContext, player: Player, island: Island) {
        render.slot(2, 2)
            .renderWith {
                itemStack(Material.ENDER_PEARL) {
                    meta<ItemMeta> {
                        name("<yellow>Teleporte para a ilha!")
                    }
                }
            }
            .onClick { click ->
                val spawn = island.spawn.toBukkit()

                if (spawn == null) {
                    player.sendRichMessage("<red>Erro: mundo não carregado.")
                    return@onClick
                }

                player.teleport(spawn.add(0.0, 2.0, 0.0))
                render.closeForPlayer()
            }
    }

    private fun setupBorderUpgradeSlot(render: RenderContext, player: Player, island: Island) {
        render.slot(2, 7)
            .renderWith {
                itemStack(Material.BARRIER) {
                    meta<ItemMeta> {
                        name("<yellow>Upar a borda!")
                        setLore { buildBorderLore(island, player).forEach { +it } }
                    }
                }
            }
            .onClick { click ->
                handleBorderUpgrade(render, player, island)
            }
    }

    private fun setupPlantingSpeedSlot(render: RenderContext, player: Player, island: Island) {
        render.slot(2, 6)
            .renderWith {
                itemStack(Material.WHEAT_SEEDS) {
                    meta<ItemMeta> {
                        name("<yellow>Upar velocidade de plantação!")
                        setLore { buildPlantingSpeedLore(island, player).forEach { +it } }
                    }
                }
            }
            .onClick { click ->
                handlePlantingSpeedUpgrade(render, player, island)
            }
    }

    private fun buildBorderLore(island: Island, player: Player): List<String> {
        val size = island.getUpgradeLevel(IslandUpgrade.Size)
        val atMax = size == IslandUpgrade.Size.maxLevel

        val currentSize = Island.BORDER_SIZE[size]
        val nextSize = if (atMax) "Máximo" else Island.BORDER_SIZE[size + 1].toString()
        val costLine = if (atMax) "<red>N/A" else "<dark_green>$<white>${NumberFormatter.format(IslandUpgrade.Size.getPrice(size + 1))}"

        val statusLine = when {
            atMax -> "<red>Você chegou ao nível máximo."
            Currencies.has(player.uniqueId, "coins", IslandUpgrade.Size.getPrice(size + 1)) -> "<green>Clique para EVOLUIR!"
            else -> "<red>Você não possui dinheiro suficiente."
        }

        return listOf(
            "  ",
            "  <white>Tamanho atual: <gray>$currentSize",
            "  <white>Próximo: <gray>$nextSize",
            " ",
            "  <white>Custo: $costLine",
            "",
            statusLine
        )
    }

    private fun buildPlantingSpeedLore(island: Island, player: Player): List<String> {
        val level = island.getUpgradeLevel(IslandUpgrade.CropGrowth)
        val atMax = level == IslandUpgrade.CropGrowth.maxLevel

        val nextSpeed = if (atMax) "Máximo" else "${level + 1}"
        val costLine = if (atMax) "<red>N/A" else "<dark_green>$<white>${NumberFormatter.format(IslandUpgrade.CropGrowth.getPrice(level + 1))}"

        val statusLine = when {
            atMax -> "<red>Você chegou ao nível máximo."
            Currencies.has(player.uniqueId, "coins", IslandUpgrade.CropGrowth.getPrice(level + 1)) -> "<green>Clique para EVOLUIR!"
            else -> "<red>Você não possui dinheiro suficiente."
        }

        return listOf(
            "  ",
            "  <white>Velocidade atual: <gray>$level",
            "  <white>Próxima: <gray>$nextSpeed",
            " ",
            "  <white>Custo: $costLine",
            "",
            statusLine
        )
    }

    private fun handleBorderUpgrade(render: RenderContext, player: Player, island: Island) {
        val size = island.getUpgradeLevel(IslandUpgrade.Size)

        if (size == IslandUpgrade.Size.maxLevel) {
            player.sendRichMessage("<red>Você já chegou ao nível máximo.")
            return
        }

        val price = IslandUpgrade.Size.getPrice(size + 1)

        if (!Currencies.has(player.uniqueId, "coins", price)) {
            player.sendRichMessage("<red>Você não possui dinheiro suficiente para upar!")
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        Currencies.withdraw(player.uniqueId, "coins", price)
        island.upgradeBorderSize()
        IslandManager.markDirty(island.uuid)
        player.sendRichMessage("<green>Borda evoluída com sucesso! <dark_gray>(${Island.BORDER_SIZE[size]} -> ${Island.BORDER_SIZE[size + 1]})")
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        render.closeForPlayer()
    }

    private fun handlePlantingSpeedUpgrade(render: RenderContext, player: Player, island: Island) {
        val level = island.getUpgradeLevel(IslandUpgrade.CropGrowth)

        if (level == IslandUpgrade.CropGrowth.maxLevel) {
            player.sendRichMessage("<red>Você já chegou ao nível máximo.")
            return
        }

        val price = IslandUpgrade.CropGrowth.getPrice(level + 1)

        if (!Currencies.has(player.uniqueId, "coins", price)) {
            player.sendRichMessage("<red>Você não possui dinheiro suficiente para upar!")
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        Currencies.withdraw(player.uniqueId, "coins", price)
        island.setUpgradeLevel(IslandUpgrade.CropGrowth, level + 1)
        IslandManager.markDirty(island.uuid)

        player.sendRichMessage("<green>Velocidade de plantação evoluída com sucesso!")
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        render.closeForPlayer()
    }

}