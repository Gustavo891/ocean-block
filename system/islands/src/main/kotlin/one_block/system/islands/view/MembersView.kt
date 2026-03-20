package one_block.system.islands.view

import de.rapha149.signgui.SignGUI
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.item_builder.*
import one_block.libs.item_builder.Items.createEmptyRender
import one_block.system.currencies.Currencies
import one_block.system.islands.IslandsSystem
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.system.islands.island.IslandMember
import one_block.system.islands.island.IslandUpgrade
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta

class MembersView : View() {

    private val islandState = initialState<Island>()

    private val pagination = lazyPaginationState(
        { ctx ->
            val island = islandState.get(ctx)
            island?.members?.values?.toMutableList()
        }
    ) { context, builder, index, value ->
        builder.renderWith { createMemberItem(value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.layout(
            "  OOOOO  ",
            "  OOOOO  ",
            "  OOOOO  ",
            "    v+   "
        )
        config.title(
            MiniMessage.miniMessage()
                .deserialize("<shift:-8><glyph:island_members><font:oneblock:gui><shift:-170><gray>Ilha > <dark_gray>Membros")
        )
    }

    override fun onFirstRender(render: RenderContext) {

        val island = islandState.get(render)

        render.createEmptyRender('v') {
            render.openForPlayer(IslandView::class.java)
        }

        render.layoutSlot('+')
            .renderWith { itemStack(Material.PAPER) {
                meta {
                    itemModel = NamespacedKey("oneblock", "blank")
                    name("<green>Adicionar Membro")
                    addLore {
                        +"<white>Limite: <gray>${island.members.count()}/${((island.getUpgradeLevel(IslandUpgrade.Members) + 1) * 3)}"
                    }
                }
            } }
            .onClick { click ->
                click.closeForPlayer()
                addMember(click, island)
            }

    }

    fun createMemberItem(member: IslandMember) = itemStack(Material.PLAYER_HEAD) {
        meta<ItemMeta> {
            name("<yellow>${member.name}")
            addLore {
                + "<white>Permissão: <gray> -/-"
            }
        }

    }

    fun addMember(ctx: Context, island: Island) {
        SignGUI.builder()
            .setLines(
                "",
                "^^^^^^^^^^",
                "Digite o nome",
                "do jogador"
            )
            .setHandler { player, result ->
                val playerName = result.getLine(2) ?: return@setHandler emptyList()

                val target = Bukkit.getPlayer(playerName)
                if (target == null || !target.isOnline) {
                    player.sendRichMessage("<red>Jogador não encontrado ou offline.")
                    return@setHandler emptyList()
                }

                if (island.ownerUuid != target.uniqueId) {
                    player.sendRichMessage("<red>Você não é o dono desta ilha!")
                    return@setHandler emptyList()
                }

                if (player.name.equals(target.name, true)) {
                    player.sendRichMessage("<red>Você quer se adicionar? Não entendi.")
                    return@setHandler emptyList()
                }

                if (island.members.size >= (island.getUpgradeLevel(IslandUpgrade.Members, 1) * 3)) {
                    player.sendRichMessage("<red>Sua ilha está lotada de membros.")
                    return@setHandler emptyList()
                }

                if (!island.addMember(target.name, IslandMember(target.name, mutableSetOf()))) {
                    player.sendRichMessage("<green>Este jogador já está adicionado")
                    return@setHandler emptyList()
                }

                player.sendRichMessage("<green>${target.name} foi adicionado como membro!")

                emptyList()
            }
            .build()
            .open(ctx.player)
    }

}