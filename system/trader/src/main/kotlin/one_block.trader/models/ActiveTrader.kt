package one_block.trader.models

import java.util.UUID

data class ActiveTrader(
    val islandId: UUID,
    val shopItems: MutableList<TraderItem>,
    val spawnTime: Long,
) {
}