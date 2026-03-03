package one_block.system.items.tasks

import one_block.system.items.SpecialItemSystem
import one_block.system.items.sell.SellBeaconManager
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import kotlin.math.sin

class AnimationTask: BukkitRunnable() {

    override fun run() {
        SellBeaconManager.hopperbyUUID.forEach { it ->
            val beacon = it.value
            val display = beacon.display ?: return@forEach

            beacon.tick++
            val tick = beacon.tick.toDouble()

            val yaw = (tick * 2f) % 360f
            val transformation = display.transformation
            val rotate = Quaternionf().rotateY(Math.toRadians(yaw).toFloat())
            display.transformation = Transformation(
                transformation.translation,
                rotate,
                transformation.scale,
                transformation.rightRotation
            )

            val baseY = beacon.originalY
            val offsetY = sin(Math.toRadians(tick * 3)) * 0.15

            val loc = display.location
            display.teleport(loc.clone().also {
                l -> l.y = baseY + offsetY
            })

         }
    }

    init {
        runTaskTimer(SpecialItemSystem.plugin, 0L, 1L)
    }

}
