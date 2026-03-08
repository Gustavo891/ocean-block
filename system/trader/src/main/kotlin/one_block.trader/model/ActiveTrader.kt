package one_block.trader.model

import java.util.*

data class ActiveTrader(
    val islandId: UUID,
    val shopItems: MutableList<TraderItem>,
    val spawnTime: Long,
) {
}