package ocean_block.system.pirate_raid.loot

import ocean_block.libs.instance.Instance
import ocean_block.system.pirate_raid.config.RaidConfig
import ocean_block.system.pirate_raid.crew.PirateKeys
import ocean_block.system.pirate_raid.crew.PirateKind
import ocean_block.system.pirate_raid.rules.CoinValue
import ocean_block.system.pirate_raid.rules.Drop
import ocean_block.system.pirate_raid.rules.LootRoller
import one_block.textura.api.ItemApi
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import kotlin.random.Random

object RaidLoot {

    const val COIN_ITEM = "moeda_pirata"
    const val PEARL_ITEM = "perola_pirata"

    private const val TICKS_PER_SECOND = 20L

    fun drop(location: Location, kind: PirateKind, island: UUID, raidLevel: Int, config: RaidConfig, random: Random) {
        val rolls = when (kind) {
            PirateKind.PILLAGER -> 1
            PirateKind.BRUTE -> config.bruteRolls
            PirateKind.SUPER -> config.superPirate.rolls
        }
        val stacks = LootRoller.roll(config.drops, rolls, random).flatMap { stacks(it, kind, island, raidLevel, config, random) }
        val extras = if (kind == PirateKind.SUPER) listOf(ItemStack(Material.TOTEM_OF_UNDYING)) else emptyList()
        (stacks + extras).forEach { stack -> spawn(location, stack, config) }
    }

    private fun stacks(drop: Drop, kind: PirateKind, island: UUID, level: Int, config: RaidConfig, random: Random): List<ItemStack> =
        when (drop.item) {
            LootRoller.COIN -> {
                val count = drop.amount * if (kind == PirateKind.SUPER) config.superPirate.coins else 1
                List(count) { coin(island, CoinValue.coin(config.coinMin, config.coinMax, level, config.levelFactor, random)) }
            }
            LootRoller.PEARL -> listOf(pearl(island, drop.amount))
            else -> Material.matchMaterial(drop.item)?.let { listOf(ItemStack(it, drop.amount)) }.orEmpty()
        }

    private fun coin(island: UUID, value: Int): ItemStack =
        tagged(ItemApi.create(COIN_ITEM) ?: ItemStack(Material.GOLD_NUGGET), PirateKeys.COIN, value, island)

    private fun pearl(island: UUID, amount: Int): ItemStack =
        tagged(ItemApi.create(PEARL_ITEM, amount) ?: ItemStack(Material.PRISMARINE_CRYSTALS, amount), PirateKeys.PEARL, 1, island)

    private fun tagged(stack: ItemStack, key: org.bukkit.NamespacedKey, value: Int, island: UUID): ItemStack {
        stack.editPersistentDataContainer { pdc ->
            pdc.set(key, PersistentDataType.INTEGER, value)
            pdc.set(PirateKeys.LOOT_ISLAND, PersistentDataType.STRING, island.toString())
        }
        return stack
    }

    private fun spawn(location: Location, stack: ItemStack, config: RaidConfig) {
        val item = location.world.dropItemNaturally(location, stack)
        if (!PirateKeys.isRaidLoot(stack)) return
        item.isGlowing = true
        item.setCanMobPickup(false)
        Instance.server.scheduler.runTaskLater(Instance, Runnable { if (item.isValid) item.remove() }, config.lootDespawnSeconds * TICKS_PER_SECOND)
    }
}
