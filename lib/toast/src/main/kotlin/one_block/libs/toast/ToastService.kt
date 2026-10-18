package one_block.libs.toast

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.advancements.Advancement
import com.github.retrooper.packetevents.protocol.advancements.AdvancementDisplay
import com.github.retrooper.packetevents.protocol.advancements.AdvancementHolder
import com.github.retrooper.packetevents.protocol.advancements.AdvancementProgress
import com.github.retrooper.packetevents.protocol.advancements.AdvancementType
import com.github.retrooper.packetevents.resources.ResourceLocation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAdvancements
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import one_block.libs.instance.Instance
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal object ToastService {

    private const val CRITERION = "toast"
    private const val NAMESPACE = "oneblock"
    private const val CLEANUP_DELAY_TICKS = 5L

    private val counter = AtomicLong()
    private val warned = AtomicBoolean()

    fun send(player: Player, type: ToastType, icon: ItemStack, text: Component) {
        val api = runCatching { PacketEvents.getAPI() }.getOrNull()
        if (api == null || !api.isInitialized) {
            if (warned.compareAndSet(false, true)) {
                Instance.logger.warning("PacketEvents nao inicializado; toasts desativados.")
            }
            return
        }

        val id = nextId()

        val display = AdvancementDisplay(
            text,
            Component.empty(),
            SpigotConversionUtil.fromBukkitItemStack(icon),
            frameOf(type),
            null,
            true,
            true,
            0f,
            0f
        )

        val advancement = Advancement(
            null,
            display,
            listOf(CRITERION),
            listOf(listOf(CRITERION)),
            false
        )

        val progress = AdvancementProgress(
            mapOf(CRITERION to AdvancementProgress.CriterionProgress(System.currentTimeMillis()))
        )

        api.playerManager.sendPacket(
            player,
            WrapperPlayServerUpdateAdvancements(
                false,
                listOf(AdvancementHolder(id, advancement)),
                emptySet(),
                mapOf(id to progress),
                true
            )
        )

        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            if (!player.isOnline) return@Runnable

            api.playerManager.sendPacket(
                player,
                WrapperPlayServerUpdateAdvancements(false, emptyList(), setOf(id), emptyMap(), true)
            )
        }, CLEANUP_DELAY_TICKS)
    }

    internal fun nextId(): ResourceLocation =
        ResourceLocation(NAMESPACE, "toast/${counter.incrementAndGet()}")

    internal fun frameOf(type: ToastType): AdvancementType = when (type) {
        ToastType.PHASE -> AdvancementType.TASK
    }
}
