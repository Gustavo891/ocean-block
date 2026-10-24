package ocean_block.system.islands.view

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandSetting
import org.bukkit.Sound

class SettingsView : View() {

    companion object {
        private const val COLUMNS = 7

        private const val FIRST_ROW = 1

        private const val FIRST_COLUMN = 2
    }

    private val islandState = initialState<Island>()

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(menuTitle("ilha_config", "Configurações"))
        config.layout(
            "         ",
            "         ",
            "    v    "
        )
    }

    override fun onFirstRender(render: RenderContext) {
        val island = islandState.get(render) ?: return
        val isOwner = island.ownerUuid == render.player.uniqueId

        render.layoutSlot('v')
            .renderWith { backItem() }
            .onClick { click -> click.openForPlayer(IslandView::class.java, island) }

        IslandSetting.entries.forEachIndexed { index, setting ->
            render.slot(FIRST_ROW + index / COLUMNS, FIRST_COLUMN + index % COLUMNS)
                .renderWith { settingItem(setting, island.isSettingEnabled(setting), isOwner) }
                .onClick { click ->
                    if (!isOwner) {
                        click.player.sendRichMessage("<red>Apenas o dono da ilha pode alterar as configurações.")
                        click.player.playSound(click.player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
                        return@onClick
                    }

                    val enabled = island.toggleSetting(setting)
                    click.player.playSound(
                        click.player,
                        if (enabled) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BASS,
                        1f,
                        1f
                    )
                    render.update()
                }
        }
    }

    private fun settingItem(setting: IslandSetting, enabled: Boolean, isOwner: Boolean) = itemStack(setting.icon) {
        meta {
            val status = if (enabled) "<green>Ativado" else "<red>Desativado"
            name("<yellow>${setting.displayName} <dark_gray>[$status<dark_gray>]")
            setLore {
                +"<gray>${if (enabled) setting.enabledLore else setting.disabledLore}"
                +""
                if (isOwner) {
                    +"<yellow>Clique para ${if (enabled) "desativar" else "ativar"}."
                } else {
                    +"<dark_gray>Apenas o dono pode alterar."
                }
            }
        }
    }
}
