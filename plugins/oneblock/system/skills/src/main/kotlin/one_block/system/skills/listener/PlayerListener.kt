package one_block.system.skills.listener

import one_block.system.skills.skill.SkillManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener : Listener {

    @EventHandler
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return

        SkillManager.load(event.uniqueId, event.name)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        SkillManager.unload(event.player.uniqueId)
    }
}
