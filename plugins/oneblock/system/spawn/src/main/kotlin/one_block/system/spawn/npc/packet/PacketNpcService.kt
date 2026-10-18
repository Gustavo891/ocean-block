package one_block.system.spawn.npc.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import one_block.libs.instance.Instance
import one_block.system.spawn.npc.SpawnNpc
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.atan2
import kotlin.math.sqrt
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation

internal object PacketNpcService {

    private const val VIEW_DISTANCE = 48.0

    private const val EYE_HEIGHT = 1.62

    private const val SKIN_LAYERS_INDEX = 16
    private const val PROFILE_INDEX = 17
    private const val IMMOVABLE_INDEX = 18

    private const val SKIN_LAYERS: Byte = 0x7F

    private const val TEXTURE_HOST = "http://textures.minecraft.net/texture/"
    private val TEXTURE_HASH = Regex("[0-9a-fA-F]{32,}")

    private val nextEntityId = AtomicInteger(Int.MAX_VALUE / 2)

    private class Handle(
        val npc: SpawnNpc,
        val entityId: Int,
        val uuid: UUID,
        val profile: ItemProfile,
        val location: Location,
    ) {
        val viewers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    }

    private val handles = ConcurrentHashMap<String, Handle>()
    private val byEntityId = ConcurrentHashMap<Int, String>()

    private val listening = AtomicBoolean()
    private val warned = AtomicBoolean()

    private var interactHandler: ((Player, String) -> Unit)? = null

    fun isAvailable(): Boolean {
        val api = runCatching { PacketEvents.getAPI() }.getOrNull()
        if (api != null && api.isInitialized) return true

        if (warned.compareAndSet(false, true)) {
            Instance.logger.warning("[spawn] PacketEvents nao inicializado; npcs de pacote desativados.")
        }
        return false
    }

    fun onInteract(handler: (Player, String) -> Unit) {
        interactHandler = handler
    }

    fun spawn(npc: SpawnNpc): Boolean {
        if (!isAvailable()) return false

        val location = npc.location.toBukkit() ?: return false
        despawn(npc.id)

        val uuid = UUID.randomUUID()
        val handle = Handle(
            npc = npc,
            entityId = nextEntityId.decrementAndGet(),
            uuid = uuid,
            profile = profileOf(npc, uuid),
            location = location,
        )

        handles[npc.id] = handle
        byEntityId[handle.entityId] = npc.id

        listen()
        return true
    }

    fun despawn(id: String) {
        val handle = handles.remove(id) ?: return
        byEntityId.remove(handle.entityId)

        handle.viewers.mapNotNull(Bukkit::getPlayer).forEach { hide(it, handle) }
        handle.viewers.clear()
    }

    fun despawnAll() {
        handles.keys.toList().forEach { despawn(it) }
    }

    fun forget(uuid: UUID) {
        handles.values.forEach { it.viewers.remove(uuid) }
    }

    fun tick() {
        if (handles.isEmpty()) return

        for (handle in handles.values) {
            for (player in Bukkit.getOnlinePlayers()) {
                val visible = canSee(player, handle)
                val showing = handle.viewers.contains(player.uniqueId)

                when {
                    visible && !showing -> show(player, handle)
                    !visible && showing -> hide(player, handle)
                }
            }

            if (!handle.npc.looksAtPlayers) continue

            for (viewerId in handle.viewers) {
                val viewer = Bukkit.getPlayer(viewerId) ?: continue
                face(viewer, handle)
            }
        }
    }

    private fun interact(player: Player, entityId: Int) {
        val id = byEntityId[entityId] ?: return
        val handler = interactHandler ?: return

        Bukkit.getScheduler().runTask(Instance, Runnable { handler(player, id) })
    }

    private fun canSee(player: Player, handle: Handle): Boolean {
        if (player.world != handle.location.world) return false
        if (player.gameMode == GameMode.SPECTATOR) return false

        return player.location.distanceSquared(handle.location) <= VIEW_DISTANCE * VIEW_DISTANCE
    }

    private fun show(player: Player, handle: Handle) {
        val yaw = handle.location.yaw

        send(
            player,
            WrapperPlayServerSpawnEntity(
                handle.entityId,
                handle.uuid,
                EntityTypes.MANNEQUIN,
                PacketLocation(
                    handle.location.x,
                    handle.location.y,
                    handle.location.z,
                    yaw,
                    handle.location.pitch,
                ),
                yaw,
                0,
                Vector3d(0.0, 0.0, 0.0),
            ),
        )

        send(
            player,
            WrapperPlayServerEntityMetadata(
                handle.entityId,
                listOf(
                    EntityData(SKIN_LAYERS_INDEX, EntityDataTypes.BYTE, SKIN_LAYERS),
                    EntityData(PROFILE_INDEX, EntityDataTypes.RESOLVABLE_PROFILE, handle.profile),
                    EntityData(IMMOVABLE_INDEX, EntityDataTypes.BOOLEAN, true),
                ),
            ),
        )

        send(player, WrapperPlayServerEntityHeadLook(handle.entityId, yaw))

        handle.viewers.add(player.uniqueId)
    }

    private fun hide(player: Player, handle: Handle) {
        send(player, WrapperPlayServerDestroyEntities(handle.entityId))

        handle.viewers.remove(player.uniqueId)
    }

    private fun face(viewer: Player, handle: Handle) {
        val eye = viewer.eyeLocation
        val dx = eye.x - handle.location.x
        val dy = eye.y - (handle.location.y + EYE_HEIGHT)
        val dz = eye.z - handle.location.z

        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = Math.toDegrees(-atan2(dy, horizontal)).toFloat()

        send(viewer, WrapperPlayServerEntityRotation(handle.entityId, yaw, pitch, false))
        send(viewer, WrapperPlayServerEntityHeadLook(handle.entityId, yaw))
    }

    private fun profileOf(npc: SpawnNpc, uuid: UUID): ItemProfile {
        val profile = ItemProfile("", uuid, mutableListOf())

        val texture = npc.texture
        if (!texture.isNullOrBlank()) {
            profile.addProperty(
                ItemProfile.Property("textures", texturePayload(texture, uuid), npc.textureSignature),
            )
        }

        return profile
    }

    private fun texturePayload(texture: String, uuid: UUID): String {
        val url = when {
            texture.startsWith("http") -> texture
            texture.matches(TEXTURE_HASH) -> "$TEXTURE_HOST$texture"
            else -> return texture
        }

        val id = uuid.toString().replace("-", "")
        val payload = "{\"timestamp\":${System.currentTimeMillis()},\"profileId\":\"$id\"," +
            "\"profileName\":\"npc\",\"signatureRequired\":false," +
            "\"textures\":{\"SKIN\":{\"url\":\"$url\"}}}"

        return Base64.getEncoder().encodeToString(payload.toByteArray(Charsets.UTF_8))
    }

    private fun send(player: Player, packet: PacketWrapper<*>) {
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun listen() {
        if (!listening.compareAndSet(false, true)) return

        PacketEvents.getAPI().eventManager.registerListener(
            object : PacketListenerAbstract(PacketListenerPriority.NORMAL) {
                override fun onPacketReceive(event: PacketReceiveEvent) {
                    if (event.packetType != PacketType.Play.Client.INTERACT_ENTITY) return

                    val wrapper = WrapperPlayClientInteractEntity(event)
                    if (wrapper.action != WrapperPlayClientInteractEntity.InteractAction.INTERACT) return

                    val player = event.getPlayer<Player>() ?: return
                    interact(player, wrapper.entityId)
                }
            },
        )
    }
}
