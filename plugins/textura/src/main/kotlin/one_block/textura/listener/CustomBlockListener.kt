package one_block.textura.listener

import one_block.textura.api.BlockApi
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import java.util.logging.Logger

class CustomBlockListener(private val logger: Logger) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val id = BlockApi.idFromItem(event.itemInHand) ?: return

        val data = BlockApi.blockData(id)
        if (data == null) {
            logger.warning("Bloco '$id' nao tem blockstate alocada; a colocacao foi cancelada.")
            event.isCancelled = true
            return
        }

        event.blockPlaced.setBlockData(data, false)
    }

    /**
     * `MONITOR` com `ignoreCancelled` é deliberado, apesar da convenção de não
     * agir nesse estágio.
     *
     * Sistemas que assumem a quebra de um bloco cancelam o evento e dropam à mão
     * — o bloco central da ilha faz exatamente isso, em `HIGHEST`. Qualquer
     * estágio anterior faria este handler dropar antes do cancelamento, e o
     * jogador receberia dois drops. `MONITOR` é o único ponto depois de
     * `HIGHEST`, e o `ignoreCancelled` é o que faz este handler recuar quando
     * outro sistema tomou a quebra para si. Quem toma deve consultar
     * [BlockApi.dropsAt] para saber o que dropar no lugar.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val definition = BlockApi.definitionAt(event.block) ?: return

        // Em CUSTOM o drop vanilla e um note block; em OVERRIDE e o item do bloco
        // que foi retexturado. Nenhum dos dois corresponde ao que o bloco aparenta.
        event.isDropItems = false
        if (event.player.gameMode == org.bukkit.GameMode.CREATIVE) return

        definition.dropId?.let { dropId ->
            if (BlockApi.dropFor(definition) == null) {
                logger.warning(
                    "Bloco '${definition.id}': block_drop_id '$dropId' nao existe em items nem em blocks."
                )
            }
        }

        val drops = BlockApi.dropsFor(definition)
        if (drops.isEmpty()) return

        val center = event.block.location.toCenterLocation()
        drops.forEach { event.block.world.dropItemNaturally(center, it) }
    }
}
