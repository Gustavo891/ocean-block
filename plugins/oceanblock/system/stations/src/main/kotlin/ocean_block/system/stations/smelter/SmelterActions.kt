package ocean_block.system.stations.smelter

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Lightable
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

object SmelterActions {

    fun use(player: Player, block: Block, placed: PlacedBlock, takeAll: Boolean) {
        val state = SmelterState.fromCounters(placed.counters)
        val hand = player.inventory.itemInMainHand
        val id = SmelterItems.idOf(hand)
        val next = when {
            takeAll && id == null -> SmelterRules.takeAll(state).also { (_, items) -> give(player, items) }.first
            id != null && SmelterRules.accepts(id) -> insert(player, state, SmelterRules.insertOre(state, id, hand.amount)) ?: return
            id != null && SmelterItems.isVanilla(hand) && Fuel.ticks(id) != null ->
                insert(player, state, SmelterRules.insertFuel(state, id, hand.amount)) ?: return
            id == null -> SmelterRules.takeOutput(state).also { (_, item) ->
                if (item == null) player.sendRichMessage("<gray>Coloque minério bruto e combustível.") else give(player, listOf(item))
            }.first
            else -> return player.sendRichMessage("<gray>Isso não entra na fundição.")
        }
        save(block, placed, next)
    }

    fun save(block: Block, placed: PlacedBlock, state: SmelterState) {
        placed.counters.keys.removeAll { it.startsWith(SmelterState.KEYS_PREFIX) }
        placed.counters.putAll(state.toCounters())
        PlacedBlockManager.markDirty(placed)
        (block.blockData as? Lightable)?.let { data ->
            val lit = state.burnTicks > 0
            if (data.isLit != lit) {
                data.isLit = lit
                block.setBlockData(data, false)
            }
        }
        SmelterDisplay.update(placed.uuid, block, state.input)
        if (state.burnTicks > 0) block.world.spawnParticle(Particle.FLAME, block.location.add(0.5, 1.0, 0.5), 2, 0.15, 0.05, 0.15, 0.0)
    }

    private fun insert(player: Player, before: SmelterState, result: Pair<SmelterState, Int>): SmelterState? {
        val (state, taken) = result
        if (taken == 0) {
            player.sendRichMessage(if (before.input != null || before.fuelItem != null) "<red>A fundição já está com outro material." else "<red>A fundição está cheia.")
            return null
        }
        val hand = player.inventory.itemInMainHand
        hand.amount -= taken
        player.inventory.setItem(EquipmentSlot.HAND, hand.takeIf { it.amount > 0 })
        player.playSound(player.location, Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 0.8f, 1.2f)
        return state
    }

    private fun give(player: Player, items: List<Pair<String, Int>>) {
        items.forEach { (id, amount) ->
            val stack = SmelterItems.stackOf(id, amount) ?: return@forEach
            player.inventory.addItem(stack).values.forEach { player.world.dropItemNaturally(player.location, it) }
        }
        if (items.isNotEmpty()) player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1f)
    }
}
