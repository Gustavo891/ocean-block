package ocean_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.instance.Instance
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandUpgrade
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class IslandView : View() {

    private val islandState = initialState<Island>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "    i    ",
            " t m u s ",
            " w r p q ",
        )
    }

    override fun onOpen(open: OpenContext) {
        val island = islandState.get(open)
        open.modifyConfig().title(menuTitle("Ilha de ${island.ownerName.take(MAX_NAME)}"))
    }

    override fun onFirstRender(render: RenderContext) {
        val island = islandState.get(render)

        render.layoutSlot('i').renderWith { infoItem(island) }
        render.layoutSlot('t')
            .renderWith { action(Material.GRASS_BLOCK, "Ir para a ilha", "Teleporta você para o spawn da ilha.") }
            .onClick { click ->
                click.closeForPlayer()
                island.teleportTo(click.player)
            }
        render.layoutSlot('m')
            .renderWith { action(Material.PLAYER_HEAD, "Membros", "Veja e convide quem faz parte da ilha.") }
            .onClick { click -> open(click, MembersView::class.java, island) }
        render.layoutSlot('u')
            .renderWith { action(Material.ANVIL, "Melhorias", "Evolua a borda, os membros e o gerador.") }
            .onClick { click -> open(click, UpgradeView::class.java, island) }
        render.layoutSlot('s')
            .renderWith { action(Material.COMPARATOR, "Configurações", "Ligue ou desligue comportamentos da ilha.") }
            .onClick { click -> open(click, SettingsView::class.java, island) }
        render.layoutSlot('w')
            .renderWith { action(Material.OAK_DOOR, "Visitas", "Abra a ilha para visitas e bloqueie jogadores.") }
            .onClick { click -> open(click, VisitsView::class.java, island) }
        render.layoutSlot('r')
            .renderWith { action(Material.CRAFTING_TABLE, "Receitas", "Abre as receitas de craft.") }
            .onClick { click -> runCommand(click, "receitas") }
        render.layoutSlot('p')
            .renderWith { action(Material.EXPERIENCE_BOTTLE, "Nível", "Mostra seu nível e as receitas.") }
            .onClick { click -> runCommand(click, "nivel") }
        render.layoutSlot('q')
            .renderWith { action(Material.MAP, "Missões", "Mostra sua missão e as diárias.") }
            .onClick { click -> runCommand(click, "missoes") }
    }

    private fun open(click: SlotClickContext, view: Class<out View>, island: Island) {
        click.player.playSound(click.player, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f)
        click.openForPlayer(view, island)
    }

    private fun runCommand(click: SlotClickContext, command: String) {
        val player = click.player
        click.closeForPlayer()
        Bukkit.getScheduler().runTask(Instance, Runnable { player.performCommand(command) })
    }

    private fun infoItem(island: Island): ItemStack = headOf(island.ownerName) {
        name("<yellow>Ilha de ${island.ownerName}")
        setLore {
            +"<gray>Dono: <white>${island.ownerName}"
            +"<gray>Membros: <white>${island.members.size}/${island.maxMembers()}"
            +"<gray>Borda: <white>${island.borderSize()} blocos"
            +"<gray>Gerador: <white>nível ${island.getUpgradeLevel(IslandUpgrade.Generator)}"
            +"<gray>Visitas: ${if (island.isPublic) "<green>Pública" else "<red>Privada"}"
        }
    }

    private fun action(material: Material, title: String, description: String): ItemStack = itemStack(material) {
        meta {
            name("<yellow>$title")
            setLore { +"<gray>$description" }
            addItemFlags(*ItemFlag.entries.toTypedArray())
        }
    }

    private companion object {
        const val MAX_NAME = 16
    }
}
