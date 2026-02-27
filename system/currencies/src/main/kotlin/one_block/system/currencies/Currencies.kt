package one_block.system.currencies

import one_block.system.currencies.currency.CurrencyManager
import java.util.*

object Currencies {

    fun deposit(uuid: UUID, id: String, amount: Double) {
        val walletManager = CurrencyManager.find(id) ?: return
        val wallet = walletManager.find(uuid) ?: return

        wallet.balance += amount
        walletManager.markDirty(uuid)
    }

    fun withdraw(uuid: UUID, id: String, amount: Double) {
        val walletManager = CurrencyManager.find(id) ?: return
        val wallet = walletManager.find(uuid) ?: return
        wallet.balance -= amount
        walletManager.markDirty(uuid)
    }

    fun has(uuid: UUID, id: String, amount: Double): Boolean {
        val walletManager = CurrencyManager.find(id) ?: return false
        val wallet = walletManager.find(uuid) ?: return false

        return wallet.balance >= amount
    }

    fun findCurrency(id: String) = CurrencyManager.find(id)
}