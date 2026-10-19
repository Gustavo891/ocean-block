package one_block.textura.api

import one_block.textura.notify.NotificationLine
import one_block.textura.notify.icon.ItemIconRegistry
import one_block.textura.notify.service.NotificationService
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object NotificationApi {

    @Volatile
    private var service: NotificationService? = null

    internal fun init(service: NotificationService) {
        this.service = service
    }

    fun push(player: Player, region: String, line: NotificationLine) {
        service?.push(player, region, line)
    }

    fun clear(player: Player, region: String) {
        service?.clear(player, region)
    }

    fun registerItemIcons(materials: Collection<String>) {
        val invalid = ItemIconRegistry.register(materials)
        if (invalid.isNotEmpty()) {
            Bukkit.getLogger().warning("[notificacoes] Materiais de ícone inválidos ignorados: ${invalid.joinToString()}")
        }
    }
}
