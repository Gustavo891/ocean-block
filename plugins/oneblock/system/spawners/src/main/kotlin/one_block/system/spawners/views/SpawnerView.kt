package one_block.system.spawners.views

import com.destroystokyo.paper.profile.ProfileProperty
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import one_block.libs.formatter.Mini
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.libs.item_builder.name
import one_block.libs.item_builder.setLore
import one_block.system.spawners.rules.StackRules
import one_block.system.spawners.models.PlacedSpawner
import one_block.system.spawners.manager.PlacedSpawnerManager
import one_block.system.spawners.SpawnersSystem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.UUID

class SpawnerView : View() {

    private val spawnerState = initialState<PlacedSpawner>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(
            Mini.mm
                .deserialize("<white><shift:-8><glyph:spawner><font:oneblock:gui><shift:-170><dark_gray>Gerador:")
        )
        config.layout(
            "  T I 6  "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val placed = spawnerState.get(render)

        render.layoutSlot('I').renderWith { statusItem(placed) }

        render.layoutSlot('T')
            .renderWith { toggleItem(placed) }
            .onClick { click ->
                if (!click.requireAlive(placed)) return@onClick
                placed.enabled = !placed.enabled
                PlacedSpawnerManager.saveNow(placed)
                click.update()
            }

        render.layoutSlot('6')
            .renderWith { withdrawItem() }
            .onClick { click -> withdraw(click, placed) }

    }

    private fun withdraw(click: SlotClickContext, placed: PlacedSpawner) {
        if (!click.requireAlive(placed)) return

        var quantidade = 1

        if(click.isRightClick) quantidade = placed.stackSize

        val player = click.player
        val type = placed.type()
        val space = freeSpaceFor(player, type.name)

        val result = StackRules.withdraw(placed.stackSize, quantidade, space)
        if (result.withdrawn == 0) {
            player.sendRichMessage("<red>Sem espaço no inventário.")
            return
        }

        placed.stackSize = result.newStack
        player.inventory.addItem(SpawnersSystem.build(type, result.withdrawn))

        if (result.newStack <= 0) {
            val block = placed.location.toBukkit()?.block
            PlacedSpawnerManager.unregister(placed.location)
            block?.type = Material.AIR
            player.sendRichMessage("<green>Spawner recolhido por inteiro.")
            click.closeForPlayer()
            return
        }

        PlacedSpawnerManager.saveNow(placed)
        if (result.withdrawn < quantidade) {
            player.sendRichMessage("<yellow>Retirado só <white>${result.withdrawn}<yellow>: inventário cheio.")
        }
        click.update()
    }

    private fun freeSpaceFor(player: Player, typeName: String): Int {
        var space = 0
        player.inventory.storageContents.forEach { item ->
            if (item == null || item.type.isAir) {
                space += 64
                return@forEach
            }
            val itemType = SpawnersSystem.typeOf(item) ?: return@forEach
            if (itemType.name == typeName) space += (item.maxStackSize - item.amount).coerceAtLeast(0)
        }
        return space
    }

    private fun statusItem(placed: PlacedSpawner): ItemStack {
        val type = placed.type()
        return itemStack(Material.PLAYER_HEAD) {
            meta<SkullMeta> {
                playerProfile = Bukkit.createProfile(UUID.randomUUID()).apply {
                    setProperty(ProfileProperty("textures", type.headTexture))
                }
                name("<white>${type.displayName}")
                setLore {
                    +""
                    +" <dark_gray>▸ <white>Empilhados: <gray>${placed.stackSize}/${StackRules.MAX_STACK}"
                    +" <dark_gray>▸ <white>Mobs vivos: <yellow>${placed.liveMobs}/${type.maxLiveMobs}"
                    +""
                    +"  <white>Status: <gray>${if (placed.enabled) "<green>■ Ligado" else "<red>■ Desligado"}"
                    +""
                }
            }
        }
    }

    private fun toggleItem(placed: PlacedSpawner) =
        itemStack(Material.PAPER) {
            meta {
                itemModel = NamespacedKey("oneblock", if (placed.enabled) "on" else "off")
                name(if (placed.enabled) "<green>Ligado" else "<red>Desligado")
                setLore {
                    +"<white><glyph:mouse_right> <gray>Clique <white>para ${if (placed.enabled) "desligar" else "ligar"}."
                }
            }
        }

    private fun withdrawItem() = itemStack(Material.PAPER) {
        meta {
            itemModel = NamespacedKey("oneblock", "blank")
            name("<white>Remoção")
            setLore {
                +"<gray>Escolha a quantidade de spawners"
                +"<gray>que deseja retirar."
                +""
                +"<white><glyph:mouse_left> <gray>Clique <white>para retirar <yellow>x1"
                +"<white><glyph:mouse_right> <gray>Clique <white>para retirar <yellow>tudo"
            }
        }
    }

    private fun SlotClickContext.requireAlive(placed: PlacedSpawner): Boolean {
        if (PlacedSpawnerManager.isRegistered(placed)) return true
        player.sendRichMessage("<red>Este spawner não existe mais.")
        closeForPlayer()
        return false
    }
}
