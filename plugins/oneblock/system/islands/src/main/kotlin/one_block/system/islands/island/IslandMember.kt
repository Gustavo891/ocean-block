package one_block.system.islands.island

import java.util.*

data class IslandMember(
    var name: String,
    var permissions: MutableSet<IslandMemberPermission>,
)
