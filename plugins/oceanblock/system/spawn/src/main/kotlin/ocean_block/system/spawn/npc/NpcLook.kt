package ocean_block.system.spawn.npc

import io.papermc.paper.entity.LookAnchor
import ocean_block.libs.instance.Instance
import ocean_block.system.spawn.npc.packet.PacketNpcService
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

internal object NpcLook {

    private const val INTERVAL_TICKS = 4L
    private const val RADIUS = 8.0
    private val RADIUS_SQUARED = RADIUS * RADIUS

    private var task: BukkitTask? = null

    fun start() {
        stop()
        task = Bukkit.getScheduler().runTaskTimer(Instance, Runnable { tick() }, INTERVAL_TICKS, INTERVAL_TICKS)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun tick() {
        PacketNpcService.tick()

        for (npc in NpcManager.all()) {
            if (npc.type == NpcType.PACKET_PLAYER) continue
            if (!npc.looksAtPlayers) continue

            val entity = NpcManager.entityOf(npc.id) ?: continue
            val target = nearestPlayer(entity)

            if (target == null) {
                reset(npc, entity)
                continue
            }

            face(npc, entity, target)
        }
    }

    private fun face(npc: SpawnNpc, entity: Entity, target: Player) {
        val eye = target.eyeLocation

        entity.lookAt(eye.x, eye.y, eye.z, LookAnchor.EYES)

        if (npc.type == NpcType.MODEL_ENGINE) {
            NpcModel.face(npc.id, entity.location.yaw)
        }
    }

    private fun reset(npc: SpawnNpc, entity: Entity) {
        val yaw = npc.location.yaw
        val pitch = npc.location.pitch

        if (entity.location.yaw == yaw && entity.location.pitch == pitch) return

        entity.setRotation(yaw, pitch)

        if (npc.type == NpcType.MODEL_ENGINE) {
            NpcModel.face(npc.id, yaw)
        }
    }

    private fun nearestPlayer(entity: Entity): Player? =
        entity.world.players
            .filter { it.gameMode != org.bukkit.GameMode.SPECTATOR }
            .filter { it.location.distanceSquared(entity.location) <= RADIUS_SQUARED }
            .minByOrNull { it.location.distanceSquared(entity.location) }
}
