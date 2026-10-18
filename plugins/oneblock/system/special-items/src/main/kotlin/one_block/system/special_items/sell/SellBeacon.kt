package one_block.system.special_items.sell

import one_block.libs.coordinate.SimpleLocation
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import java.util.UUID

data class SellBeacon(
    val uuid: UUID = UUID.randomUUID(),
    val coordinate: SimpleLocation,
    val ownerUUID: UUID,
    val islandUUID: UUID,
    var coinsAmount: Double = 0.0,
) {

    @Transient
    var display: ItemDisplay? = null

    @Transient
    var tick: Int = 0

    @Transient
    var originalY: Double = 0.0

    @Transient
    var interaction: Interaction? = null

    fun key(): String = "${coordinate.world}:${coordinate.x}:${coordinate.y}:${coordinate.z}"
}
