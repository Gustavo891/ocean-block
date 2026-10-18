package one_block.libs.rewards

import one_block.libs.formatter.Mini
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object RewardGranter {

    fun grant(rewards: List<Reward>, players: List<Player>) {
        if (players.isEmpty()) return

        for (reward in rewards) {
            when (reward.type) {
                RewardType.Message -> giveMessage(reward, players)
                RewardType.Item -> giveItem(reward, players)
                RewardType.Command -> runCommand(reward, players)
            }
        }
    }

    fun grant(rewards: List<Reward>, player: Player) = grant(rewards, listOf(player))

    private fun giveMessage(reward: Reward, players: List<Player>) {
        val raw = reward.value ?: return
        val component = Mini.deserialize(raw)
        players.forEach { it.sendMessage(component) }
    }

    private fun giveItem(reward: Reward, players: List<Player>) {
        val material = reward.material ?: return
        val amount = reward.amount ?: 1

        for (player in players) {
            val leftover = player.inventory.addItem(ItemStack(material, amount))
            leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
        }
    }

    private fun runCommand(reward: Reward, players: List<Player>) {
        val template = reward.value ?: return
        val console = Bukkit.getConsoleSender()

        for (player in players) {
            val command = template
                .replace("%player%", player.name)
                .replace("{player}", player.name)
            Bukkit.dispatchCommand(console, command)
        }
    }
}
