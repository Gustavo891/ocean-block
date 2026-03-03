package one_block.system.items.sell

import org.bukkit.Location
import org.bukkit.Particle

data class ParticleBeam(
    private val startLocation: Location,
    private val targetLocation: Location,
    private val particle: Particle,
    private var speed: Double = 0.0,
    private val onFinish: () -> Unit
) {
    private val maxLength: Double = startLocation.distance(targetLocation)
    private val direction = targetLocation.clone().subtract(startLocation).toVector().normalize()
    private val position = startLocation.clone()

    fun tick(): Boolean {
        if (speed >= maxLength) {
            onFinish.invoke()
            return false
        }

        speed += 0.3

        val x = direction.x * speed
        val y = direction.y * speed
        val z = direction.z * speed

        position.set(startLocation.x + x, startLocation.y + y, startLocation.z + z)
        startLocation.world.spawnParticle(particle, position, 1, 0.0, 0.0, 0.0, 0.0)

        return true
    }
}