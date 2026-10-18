package ocean_block.system.skills.feedback

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import net.kyori.adventure.text.Component
import ocean_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

internal object XpTextDisplay {

    private const val LIFETIME_TICKS = 30

    private const val INTERPOLATION_DELAY_INDEX = 8
    private const val TRANSFORM_DURATION_INDEX = 9
    private const val TRANSLATION_INDEX = 11
    private const val SCALE_INDEX = 12
    private const val BILLBOARD_INDEX = 15
    private const val TEXT_INDEX = 23
    private const val BACKGROUND_INDEX = 25

    private const val BILLBOARD_CENTER: Byte = 3
    private const val TRANSPARENT_BACKGROUND = 0

    private const val START_SCALE = 0.55f
    private const val END_SCALE = 0.05f

    private const val RISE = 0.9f

    private const val HEIGHT_OFFSET = 0.4

    private const val JITTER = 0.25

    private val ids = AtomicInteger(Int.MAX_VALUE - 100_000)
    private val warned = AtomicBoolean()

    fun send(player: Player, text: Component, at: Location) {
        val api = runCatching { PacketEvents.getAPI() }.getOrNull()

        if (api == null || !api.isInitialized) {
            if (warned.compareAndSet(false, true)) {
                Instance.logger.warning("PacketEvents nao inicializado; indicador de XP desativado.")
            }
            return
        }

        val id = ids.decrementAndGet()
        val where = at.clone().add(
            Random.nextDouble(-JITTER, JITTER),
            HEIGHT_OFFSET,
            Random.nextDouble(-JITTER, JITTER),
        )

        val spawn = WrapperPlayServerSpawnEntity(
            id,
            UUID.randomUUID(),
            EntityTypes.TEXT_DISPLAY,
            PacketLocation(where.x, where.y, where.z, 0f, 0f),
            0f,
            0,
            Vector3d(0.0, 0.0, 0.0),
        )

        val initial = WrapperPlayServerEntityMetadata(
            id,
            listOf<EntityData<*>>(
                EntityData(BILLBOARD_INDEX, EntityDataTypes.BYTE, BILLBOARD_CENTER),
                EntityData(TEXT_INDEX, EntityDataTypes.ADV_COMPONENT, text),
                EntityData(BACKGROUND_INDEX, EntityDataTypes.INT, TRANSPARENT_BACKGROUND),
                EntityData(SCALE_INDEX, EntityDataTypes.VECTOR3F, Vector3f(START_SCALE, START_SCALE, START_SCALE)),
                EntityData(INTERPOLATION_DELAY_INDEX, EntityDataTypes.INT, 0),
                EntityData(TRANSFORM_DURATION_INDEX, EntityDataTypes.INT, LIFETIME_TICKS),
            ),
        )

        val manager = api.playerManager
        manager.sendPacket(player, spawn)
        manager.sendPacket(player, initial)

        Bukkit.getScheduler().runTaskLater(
            Instance,
            Runnable {
                if (!player.isOnline) return@Runnable

                manager.sendPacket(
                    player,
                    WrapperPlayServerEntityMetadata(
                        id,
                        listOf<EntityData<*>>(
                            EntityData(TRANSLATION_INDEX, EntityDataTypes.VECTOR3F, Vector3f(0f, RISE, 0f)),
                            EntityData(
                                SCALE_INDEX,
                                EntityDataTypes.VECTOR3F,
                                Vector3f(END_SCALE, END_SCALE, END_SCALE),
                            ),
                        ),
                    ),
                )
            },
            1L,
        )

        Bukkit.getScheduler().runTaskLater(
            Instance,
            Runnable {
                if (!player.isOnline) return@Runnable
                manager.sendPacket(player, WrapperPlayServerDestroyEntities(id))
            },
            LIFETIME_TICKS.toLong(),
        )
    }
}
