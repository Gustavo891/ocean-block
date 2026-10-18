package ocean_block.system.currencies.expansion

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.system.currencies.wallet.WalletManager
import org.bukkit.entity.Player

class WalletExpansion(
    private val walletManager: WalletManager,
) : PlaceholderExpansion() {
    override fun getIdentifier(): String = "wallet${walletManager.currency.id}"
    override fun getAuthor(): String = "ocoisaruim"
    override fun getVersion(): String = "1.0"

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        val wallet = walletManager.find(player.uniqueId) ?: return "aaaaaaa"
        return NumberFormatter.format(wallet.balance)
    }
}
