package ocean_block.system.currencies.wallet

import java.util.*

data class Wallet(
    val uuid: UUID,
    val name: String,
    var balance: Double,
)
