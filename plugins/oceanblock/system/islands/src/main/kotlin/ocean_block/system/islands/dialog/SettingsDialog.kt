package ocean_block.system.islands.dialog

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandSetting
import ocean_block.system.islands.view.SettingsView
import one_block.textura.api.DialogBinds
import org.bukkit.Sound
import org.bukkit.entity.Player

object SettingsDialog {

    private const val SCREEN = "ilha_configuracoes"

    fun open(player: Player, island: Island, viewFrame: ViewFrame) {
        IslandScreens.show(player, SCREEN, { viewFrame.open(SettingsView::class.java, it, island) }) { binds ->
            bind(binds, island, viewFrame)
        }
    }

    private fun bind(binds: DialogBinds, island: Island, viewFrame: ViewFrame) {
        var notice = ""
        binds.text("ligadas") { "${IslandSetting.entries.count(island::isSettingEnabled)} DE ${IslandSetting.entries.size} LIGADAS" }
        binds.list("aviso") { if (notice.isEmpty()) emptyList() else listOf(mapOf("texto" to notice)) }
        binds.list("ajustes") { IslandRows.settings(island) }
        binds.listButton("alternar") { click, index ->
            val setting = IslandSetting.entries.getOrNull(index) ?: return@listButton
            val viewer = click.player
            if (!IslandScreens.isOwner(island, viewer)) {
                notice = "SÓ O DONO DA ILHA PODE ALTERAR."
                viewer.playSound(viewer, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
                return@listButton
            }
            val enabled = island.toggleSetting(setting)
            viewer.playSound(viewer, if (enabled) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
        }
        binds.button("voltar") { click -> IslandScreens.later(click.player) { IslandDialog.open(it, island, viewFrame) } }
    }
}
