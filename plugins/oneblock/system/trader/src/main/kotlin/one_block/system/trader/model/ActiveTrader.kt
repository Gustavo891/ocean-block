package one_block.system.trader.model

import java.util.*

data class ActiveTrader(
    val islandId: UUID,
    val shopItems: MutableList<TraderItem>,
    val spawnTime: Long,
) {
}
