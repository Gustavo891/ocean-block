package ocean_block.system.islands.island

import ocean_block.system.currencies.Currencies
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player

object UpgradePurchase {

    private const val COINS = "coins"

    fun buy(player: Player, island: Island, upgrade: IslandUpgrade): Boolean {
        val level = island.getUpgradeLevel(upgrade)
        if (level >= upgrade.maxLevel) {
            player.sendRichMessage("<red>Você já chegou ao nível máximo.")
            return false
        }
        val price = upgrade.getPrice(level)
        if (!Currencies.has(player.uniqueId, COINS, price)) {
            player.sendRichMessage("<red>Você não possui coins suficientes para melhorar.")
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return false
        }
        Currencies.withdraw(player.uniqueId, COINS, price)
        upgrade.execute(player, island)
        IslandManager.markDirty(island.uuid)
        Bukkit.getPluginManager().callEvent(IslandUpgradeEvent(island, upgrade, island.getUpgradeLevel(upgrade)))
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        return true
    }

    fun affordable(player: Player, price: Double): Boolean = Currencies.has(player.uniqueId, COINS, price)
}
