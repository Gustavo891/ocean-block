package one_block.textura.api

import one_block.textura.notify.NotificationLine
import one_block.textura.notify.icon.ItemIconRegistry
import one_block.textura.notify.sample.NotificationSamples
import one_block.textura.notify.service.NotificationService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.function.Consumer

object NotificationApi {

    @Volatile
    private var service: NotificationService? = null

    private val samples = NotificationSamples<Player>()

    internal fun init(service: NotificationService) {
        this.service = service
    }

    fun push(player: Player, region: String, line: NotificationLine) {
        service?.push(player, region, line)
    }

    fun clear(player: Player, region: String) {
        service?.clear(player, region)
    }

    fun registerSample(id: String, sample: Consumer<Player>) {
        samples.register(id, sample)
    }

    internal fun sampleIds(): List<String> = samples.ids()

    internal fun sendSample(player: Player, id: String): Boolean = samples.send(id, player)

    fun registerItemIcons(materials: Collection<String>) {
        val invalid = ItemIconRegistry.register(materials)
        if (invalid.isNotEmpty()) {
            Bukkit.getLogger().warning("[notificacoes] Materiais de ícone inválidos ignorados: ${invalid.joinToString()}")
        }
    }
}
