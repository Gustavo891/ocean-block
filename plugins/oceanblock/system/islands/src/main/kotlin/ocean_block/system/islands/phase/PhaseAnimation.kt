package ocean_block.system.islands.phase

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import ocean_block.libs.formatter.Mini
import ocean_block.libs.instance.Instance
import ocean_block.libs.toast.ToastApi
import ocean_block.libs.toast.ToastType
import ocean_block.system.islands.island.Island
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.time.Duration
import java.util.UUID

object PhaseAnimation {

    const val FREEZE_TICKS = 60L

    private const val STEP_TICKS = 2L
    private const val START_SCALE = 0.08f
    private const val END_SCALE = 1.0f
    private const val TURNS = 2f

    private val mini = Mini.mm

    private val displays = mutableMapOf<UUID, BlockDisplay>()

    private val fireworkColors = listOf(
        Color.YELLOW, Color.ORANGE, Color.LIME, Color.AQUA, Color.FUCHSIA, Color.WHITE
    )

    fun start(island: Island, material: Material, players: List<Player>) {
        val loc = island.center.toBukkit() ?: return
        val world = loc.world ?: return

        loc.block.type = Material.BARRIER

        val display = world.spawn(loc.clone().add(0.5, 0.0, 0.5), BlockDisplay::class.java) {
            it.block = material.createBlockData()
            it.isPersistent = false
            it.interpolationDelay = 0
            it.interpolationDuration = STEP_TICKS.toInt()
            it.transformation = transformationOf(START_SCALE, 0f)
        }

        displays[island.uuid]?.remove()
        displays[island.uuid] = display

        players.forEach { it.playSound(it.location, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f) }

        grow(display, loc)
    }

    fun finish(island: Island, completed: Phase, next: Phase, players: List<Player>) {
        displays.remove(island.uuid)?.remove()

        if (players.isNotEmpty()) {
            showTitle(next, players)
            playSound(players)
            ToastApi.send(players, ToastType.PHASE, completed.iconMaterial, completed.prettyName)
        }

        val loc = island.center.toBukkit() ?: return

        revealParticles(loc)
        spawnFireworks(loc)
    }

    fun cancel(islandId: UUID) {
        displays.remove(islandId)?.remove()
    }

    private fun grow(display: BlockDisplay, loc: Location) {
        val world = loc.world ?: return
        val center = loc.clone().add(0.5, 0.0, 0.5)

        var elapsed = 0L
        lateinit var task: BukkitTask

        task = Bukkit.getScheduler().runTaskTimer(Instance, Runnable {
            if (!display.isValid || elapsed >= FREEZE_TICKS) {
                task.cancel()
                return@Runnable
            }

            elapsed += STEP_TICKS

            val progress = (elapsed.toFloat() / FREEZE_TICKS).coerceIn(0f, 1f)
            val eased = progress * progress
            val scale = START_SCALE + (END_SCALE - START_SCALE) * eased
            val angle = TURNS * 2f * Math.PI.toFloat() * progress

            display.interpolationDelay = 0
            display.interpolationDuration = STEP_TICKS.toInt()
            display.transformation = transformationOf(scale, angle)

            val at = center.clone().add(0.0, scale.toDouble() / 2.0, 0.0)
            world.spawnParticle(Particle.END_ROD, at, 2, 0.25, 0.1, 0.25, 0.004)
            world.spawnParticle(Particle.ENCHANT, at, 6, 0.35, 0.2, 0.35, 0.35)
        }, STEP_TICKS, STEP_TICKS)
    }

    private fun transformationOf(scale: Float, angle: Float): Transformation {
        val rotation = Quaternionf().rotateY(angle)
        val half = rotation.transform(Vector3f(scale / 2f, scale / 2f, scale / 2f))

        val translation = Vector3f(-half.x, scale / 2f - half.y, -half.z)

        return Transformation(translation, rotation, Vector3f(scale, scale, scale), Quaternionf())
    }

    private fun revealParticles(loc: Location) {
        val world = loc.world ?: return
        val center = loc.clone().add(0.5, 0.5, 0.5)

        world.spawnParticle(Particle.FLASH, center, 1, 0.0, 0.0, 0.0, 0.0, Color.WHITE)
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 80, 0.6, 0.8, 0.6, 0.2)
        world.spawnParticle(Particle.FIREWORK, center, 40, 0.5, 0.6, 0.5, 0.1)
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 30, 0.5, 0.6, 0.5, 0.0)
        world.spawnParticle(Particle.BLOCK, center, 60, 0.4, 0.4, 0.4, 0.0, loc.block.blockData)
    }

    private fun showTitle(next: Phase, players: List<Player>) {
        val main = mini
            .deserialize("<gradient:#f7971e:#ffd200>${next.prettyName}</gradient>")
            .decoration(TextDecoration.ITALIC, false)

        val sub = Component.text("Nova fase desbloqueada!")

        val times = Title.Times.times(
            Duration.ofMillis(300),
            Duration.ofSeconds(3),
            Duration.ofMillis(600)
        )

        val title = Title.title(main, sub, times)
        players.forEach { it.showTitle(title) }
    }

    private fun playSound(players: List<Player>) {
        players.forEach {
            it.playSound(it.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
            it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        }
    }

    private fun spawnFireworks(loc: Location) {
        val world = loc.world ?: return
        val spawnAt = loc.clone().add(0.5, 1.0, 0.5)

        repeat(3) { index ->
            Bukkit.getScheduler().runTaskLater(Instance, Runnable {
                val firework = world.spawn(spawnAt, Firework::class.java)
                val meta = firework.fireworkMeta
                meta.addEffect(
                    FireworkEffect.builder()
                        .withColor(fireworkColors.random())
                        .withFade(fireworkColors.random())
                        .with(FireworkEffect.Type.entries.random())
                        .flicker(true)
                        .trail(true)
                        .build()
                )
                meta.power = 1
                firework.fireworkMeta = meta
            }, index * 6L)
        }
    }
}
