package ocean_block.system.pirate_raid.raid

import ocean_block.system.currencies.Currencies
import ocean_block.system.level.PlayerLevels
import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.pirate_raid.config.RaidConfig
import ocean_block.system.pirate_raid.rules.CoinValue
import org.bukkit.entity.Player
import kotlin.math.roundToInt

object RaidRewards {

    const val COINS = "coins"
    const val PEARLS = "perolas"

    fun pay(player: Player, raidLevel: Int, config: RaidConfig): Int {
        val coins = CoinValue.victory(config.victoryCoins, raidLevel, config.levelFactor)
        if (coins > 0) Currencies.deposit(player.uniqueId, COINS, coins.toDouble())
        val xp = (config.victoryXpFraction * LevelCurve.toNext(PlayerLevels.levelOf(player))).roundToInt()
        if (xp > 0) PlayerLevels.award(player, xp)
        return coins
    }
}
