package one_block.system.items.sell

import org.bukkit.Location
import org.bukkit.Particle

data class ParticleBeam(
    private val startLocation: Location,
    private val maxLenght: Double,
    private val particle: Particle,
    private var speed: Double = 0.5,
) {

    val position = startLocation.clone()
    val direction = startLocation.direction.normalize()

    fun tick(): Boolean {
        if (speed >= maxLenght) return true

        speed += 0.3

        val x: Double = direction.x * speed
        val y: Double = direction.y * speed + 1.5
        val z: Double = direction.z * speed

        position.add(x, y, z)
        startLocation.world.spawnParticle(particle, position, 1)
        position.subtract(x, y, z)
        return false
    }
}