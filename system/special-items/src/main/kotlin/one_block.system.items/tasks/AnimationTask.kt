package one_block.system.items.tasks

import one_block.system.items.SpecialItemSystem
import one_block.system.items.sell.SellBeaconManager
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import kotlin.math.PI
import kotlin.math.sin

class AnimationTask: BukkitRunnable() {

    private var t = 0.0

    override fun run() {
        t += 1.0 / 20.0

        SellBeaconManager.beaconbyUUID.forEach { (_, beacon) ->
            val display = beacon.display ?: return@forEach

            val degPerSec = 45.0
            val yawDeg = (t * degPerSec) % 360.0
            val rotate = Quaternionf().rotateY((yawDeg * (PI / 180.0)).toFloat())

            val tr = display.transformation
            display.transformation = Transformation(
                tr.translation,
                rotate,
                tr.scale,
                tr.rightRotation
            )

            // ---- Flutuação suave (seno em Hz)
            val baseY = beacon.originalY
            val amplitude = 0.12              // altura do “vai e vem”
            val freqHz = 0.6                  // 0.3..1.2 fica natural
            val offsetY = sin(2.0 * PI * freqHz * t) * amplitude

            val loc = display.location
            display.teleport(loc.apply { y = baseY + offsetY })
        }
    }

    init {
        runTaskTimer(SpecialItemSystem.plugin, 0L, 1L)
    }

}
