package ocean_block.system.battle_pass

import co.aikar.commands.PaperCommandManager
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.instance.Instance
import ocean_block.system.battle_pass.listener.PlayerListener
import ocean_block.system.battle_pass.challenge.ProgressManager
import ocean_block.system.battle_pass.command.BattlePassCommand
import ocean_block.system.battle_pass.command.MissionCommand
import ocean_block.system.battle_pass.player.BattlePassManager
import ocean_block.system.battle_pass.listener.ChallengeListener
import ocean_block.system.battle_pass.view.ChallengesView
import ocean_block.system.battle_pass.view.BattlePassView

object BattlePassSystem {

    lateinit var listener: ChallengeListener
        private set

    fun init(viewFrame: ViewFrame, paperCommandManager: PaperCommandManager) {
        ProgressManager.onInit()
        BattlePassManager.onInit()

        listener = ChallengeListener()

        viewFrame.with(BattlePassView())
        viewFrame.with(ChallengesView())

        paperCommandManager.registerCommand(BattlePassCommand(viewFrame))
        paperCommandManager.registerCommand(MissionCommand(viewFrame))

        with(Instance.server.pluginManager) {
            registerEvents(listener, Instance)
            registerEvents(PlayerListener(), Instance)
        }
    }
}
