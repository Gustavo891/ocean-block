package ocean_block.system.preferences

import co.aikar.commands.BaseCommand
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import org.bukkit.entity.Player

object PreferencesSystem {

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        viewFrame.with(PreferencesMenu())
        paperCommandManager.registerCommand(PrefCommand(viewFrame))
    }

    @CommandAlias("pref|prefs|preferencias")
    class PrefCommand(private val viewFrame: ViewFrame) : BaseCommand() {

        @Default
        fun onDefault(player: Player) {
            viewFrame.open(PreferencesMenu::class.java, player)
        }
    }
}
