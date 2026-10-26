package ocean_block.system.level

import ocean_block.system.craft.recipe.Recipes
import ocean_block.system.level.config.LevelConfig
import ocean_block.system.level.curve.LevelCurve
import ocean_block.system.currencies.Currencies
import ocean_block.system.level.curve.LevelUps
import ocean_block.system.level.levelup.LevelUpRewards
import ocean_block.system.level.data.LevelManager
import ocean_block.system.level.notify.LevelNotifier
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player

object PlayerLevels {

    @Volatile
    var config: LevelConfig = LevelConfig.DEFAULT
        internal set

    fun levelOf(player: Player): Int = LevelCurve.levelOf(LevelManager.xp(player.uniqueId))

    fun award(player: Player, amount: Int) {
        if (amount <= 0 || player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) return
        val before = LevelManager.xp(player.uniqueId)
        if (before >= LevelCurve.totalFor(LevelCurve.MAX_LEVEL)) return
        val after = LevelManager.setXp(player.uniqueId, before + amount)
        LevelNotifier.xp(player, (after - before).toInt())
        changed(player, LevelCurve.levelOf(before), LevelCurve.levelOf(after))
    }

    fun setXp(player: Player, xp: Long) {
        val before = LevelCurve.levelOf(LevelManager.xp(player.uniqueId))
        val after = LevelCurve.levelOf(LevelManager.setXp(player.uniqueId, xp))
        changed(player, before, after)
    }

    private fun changed(player: Player, from: Int, to: Int) {
        if (to <= from) return
        val coins = LevelUpRewards.coins(from, to)
        Currencies.deposit(player.uniqueId, "coins", coins)
        LevelNotifier.levelUp(player, to, coins, LevelUps.unlocked(from, to, Recipes.all()).map { it.name })
        Bukkit.getPluginManager().callEvent(PlayerLevelUpEvent(player, from, to))
    }
}
