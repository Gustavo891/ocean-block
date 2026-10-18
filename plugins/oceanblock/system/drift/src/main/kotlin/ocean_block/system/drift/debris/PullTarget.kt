package ocean_block.system.drift.debris

import java.util.UUID

sealed interface PullTarget {
    data class Player(val uuid: UUID) : PullTarget
    data class Net(val placedUuid: UUID) : PullTarget
}
