package ocean_block.system.battle_pass.player

import ocean_block.system.battle_pass.config.BattlePassTier
import ocean_block.system.battle_pass.dao.BattlePassRepository
import ocean_block.system.battle_pass.util.MessageUtil
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BattlePassManager {

    private val players = ConcurrentHashMap<UUID, PlayerPassData>()
    private val repository = BattlePassRepository()

    fun onInit() {
        repository.toList().forEach { players[it.playerUuid] = it }
    }

    fun get(uuid: UUID): PlayerPassData {
        return players.getOrPut(uuid) {
            val bp = repository.find(uuid)
            if (bp != null) return@getOrPut bp

            val newBp = PlayerPassData(playerUuid = uuid)
            repository.update(newBp)
            newBp
        }
    }

    internal fun save(bp: PlayerPassData) {
        repository.update(bp)
    }

    fun addXp(player: Player, amount: Int) {
        if (amount <= 0) return
        val bp = get(player.uniqueId)
        val previousTier = BattlePassTier.fromXp(bp.xp)
        bp.xp += amount
        val newTier = BattlePassTier.fromXp(bp.xp)
        repository.update(bp)

        if (newTier > previousTier) {
            player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
            player.sendRichMessage(
                MessageUtil.TIER_UNLOCKED,
                Placeholder.parsed("tier", newTier.toString()),
            )
        }
    }

    fun isPremium(player: Player): Boolean {
        return player.hasPermission("oneblock.battlepass")
    }

    fun claimFree(player: Player, tier: Int): Boolean {
        val bp = get(player.uniqueId)
        if (tier in bp.claimedFree) return false
        if (BattlePassTier.fromXp(bp.xp) < tier) return false
        val data = BattlePassTier.get(tier) ?: return false

        data.executeFreeReward(player)
        bp.claimedFree.add(tier)
        repository.update(bp)
        return true
    }

    fun claimPremium(player: Player, tier: Int): Boolean {
        val bp = get(player.uniqueId)
        if (!isPremium(player)) return false
        if (tier in bp.claimedPremium) return false
        if (BattlePassTier.fromXp(bp.xp) < tier) return false
        val data = BattlePassTier.get(tier) ?: return false

        data.executePremiumReward(player)
        bp.claimedPremium.add(tier)
        repository.update(bp)
        return true
    }
}
