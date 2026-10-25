package ocean_block.system.missions.dialog

import ocean_block.system.missions.MissionTracker
import ocean_block.system.missions.data.MissionManager
import ocean_block.system.missions.notify.MissionNotifier
import ocean_block.system.missions.rules.MissionRules
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogOpenResult
import org.bukkit.entity.Player
import java.time.LocalDateTime

object MissionDialog {

    private const val SCREEN = "missoes"
    private val TRAIL_KEYS = listOf("titulo", "texto", "contador", "xp", "barra")

    fun registerIcons() {
        DialogApi.registerItemIcons(listOf(MissionNotifier.ICON_MATERIAL))
    }

    fun open(player: Player): Boolean {
        MissionTracker.refresh(player)
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player) }
        return result == DialogOpenResult.Shown || result == DialogOpenResult.NeedsPack
    }

    private fun bind(binds: DialogBinds, player: Player) {
        val state = { MissionManager.get(player.uniqueId) }
        TRAIL_KEYS.forEach { key -> binds.text(key) { MissionLore.trail(state())[key].orEmpty() } }
        binds.list("diarias") { state().dailies.map(MissionLore::daily) }
        binds.listPage("diarias") { 0 }
        binds.text("renova") {
            if (MissionRules.trailDone(state())) MissionLore.resetIn(LocalDateTime.now(MissionTracker.zone))
            else MissionLore.LOCKED_DAILIES
        }
    }
}
