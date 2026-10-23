package ocean_block.system.research.menu

import ocean_block.system.custom_items.CustomIds
import ocean_block.system.research.data.ResearchManager
import ocean_block.system.research.model.ResearchCost
import ocean_block.system.research.model.ResearchDefinition
import ocean_block.system.research.model.ResearchStatus
import ocean_block.system.research.rules.ResearchRules
import ocean_block.system.research.tree.ResearchTree
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

object ResearchDelivery {

    private const val PLANKS = "planks"

    fun deliver(player: Player, islandUuid: UUID, def: ResearchDefinition) {
        val contents = player.inventory.storageContents
        val available = def.cost.associate { cost -> cost.key to contents.filter { matches(cost, it) }.sumOf { it?.amount ?: 0 } }
        val now = System.currentTimeMillis()
        val (taken, status) = ResearchManager.edit(islandUuid) { state ->
            ResearchRules.deliver(def, state, available, now, ResearchTree.byId) to ResearchRules.status(def, state, now)
        }
        if (taken.isEmpty()) {
            player.sendRichMessage("<gray>Você não tem nenhum item que falta para ${def.name}.")
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            return
        }
        def.cost.forEach { cost -> remove(player, cost, taken[cost.key] ?: 0) }
        ResearchManager.saveNow(islandUuid)
        val delivered = def.cost.mapNotNull { cost -> taken[cost.key]?.let { "$it ${cost.labelFor(it)}" } }.joinToString(" ∙ ")
        player.sendRichMessage("<green>Entregue: <white>$delivered</white>.")
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1f)
        when (status) {
            is ResearchStatus.Running -> player.sendRichMessage("<green>Pesquisa iniciada: <white>${def.name}</white>.")
            ResearchStatus.Queued -> player.sendRichMessage("<yellow>${def.name} entrou na fila.")
            else -> Unit
        }
    }

    private fun matches(cost: ResearchCost, stack: ItemStack?): Boolean {
        if (stack == null || stack.type.isAir || CustomIds.of(stack) != null) return false
        return if (cost.key == PLANKS) Tag.PLANKS.isTagged(stack.type) else stack.type == Material.matchMaterial(cost.key)
    }

    private fun remove(player: Player, cost: ResearchCost, amount: Int) {
        var left = amount
        val contents = player.inventory.storageContents
        contents.forEachIndexed { slot, stack ->
            if (left == 0 || stack == null || !matches(cost, stack)) return@forEachIndexed
            val used = minOf(left, stack.amount)
            stack.amount -= used
            left -= used
            contents[slot] = stack.takeIf { it.amount > 0 }
        }
        player.inventory.storageContents = contents
    }
}
