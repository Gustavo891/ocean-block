package ocean_block.system.stations.sieve

import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.stations.StationsSystem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID
import kotlin.random.Random

object SieveActions {

    private val lastStep = mutableMapOf<UUID, Long>()

    fun use(player: Player, block: Block, placed: PlacedBlock, tier: SieveTier, quick: Boolean) {
        var state = SieveProgress.fromCounters(placed.counters)
        if (state.input == null) {
            val hand = player.inventory.itemInMainHand
            val input = SieveInput.of(hand.type.name) ?: run {
                player.sendRichMessage("<gray>Use areia, cascalho ou terra na peneira.")
                return
            }
            state = SieveProgress.load(state, input, tier) ?: return
            hand.amount -= 1
            player.inventory.setItem(EquipmentSlot.HAND, hand.takeIf { it.amount > 0 })
            save(block, placed, state, tier)
            effects(block, input)
            if (!quick) return
        }

        val now = Bukkit.getCurrentTick().toLong()
        if (!SieveProgress.allowed(lastStep[player.uniqueId], now)) return
        lastStep[player.uniqueId] = now
        state = advance(block, placed, state, tier) { outputs -> SieveDelivery.deliver(player, outputs) }
        save(block, placed, state, tier)
    }

    fun advance(block: Block, placed: PlacedBlock, state: SieveState, tier: SieveTier, deliver: (List<Pair<String, Int>>) -> Unit): SieveState =
        when (val step = SieveProgress.step(state)) {
            SieveStep.Empty -> state
            is SieveStep.Advanced -> step.state.also { effects(block, checkNotNull(state.input)) }
            is SieveStep.Done -> {
                effects(block, step.input)
                deliver(SieveRoller.roll(StationsSystem.sieveTable.drops(step.input, tier), Random.Default))
                SieveProgress.EMPTY
            }
        }

    fun save(block: Block, placed: PlacedBlock, state: SieveState, tier: SieveTier) {
        placed.counters.putAll(SieveProgress.toCounters(state))
        PlacedBlockManager.markDirty(placed)
        val data = block.blockData as? Levelled ?: return
        data.level = SieveProgress.composterLevel(state, tier)
        block.setBlockData(data, false)
    }

    fun forget(id: UUID) {
        lastStep.remove(id)
    }

    private fun effects(block: Block, input: SieveInput) {
        val material = Material.matchMaterial(input.material) ?: return
        val center = block.location.add(0.5, 0.9, 0.5)
        block.world.spawnParticle(Particle.BLOCK, center, 12, 0.25, 0.05, 0.25, 0.0, material.createBlockData())
        val sound = when (input) {
            SieveInput.SAND -> Sound.BLOCK_SAND_HIT
            SieveInput.GRAVEL -> Sound.BLOCK_GRAVEL_HIT
            SieveInput.DIRT -> Sound.BLOCK_ROOTED_DIRT_HIT
        }
        block.world.playSound(center, sound, 0.8f, 1f)
    }
}
