package ocean_block.system.missions.listener

import ocean_block.libs.account.AccountResetEvent
import ocean_block.system.islands.choice.IslandChoiceEvent
import ocean_block.system.missions.MissionTracker
import ocean_block.system.missions.hud.MissionHud
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class AccountResetListener : Listener {

    @EventHandler
    fun onReset(event: AccountResetEvent) {
        MissionTracker.reset(event.player)
    }

    @EventHandler
    fun onChoice(event: IslandChoiceEvent) {
        if (event.choosing) MissionHud.suspend(event.player) else MissionHud.resume(event.player)
    }
}
