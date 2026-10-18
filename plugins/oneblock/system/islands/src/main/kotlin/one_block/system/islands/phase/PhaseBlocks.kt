package one_block.system.islands.phase

import one_block.libs.instance.Instance
import one_block.system.islands.ext.getRandomByWeight
import one_block.system.islands.ext.runNextTask
import one_block.textura.api.ItemApi
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.data.Bisected
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

object PhaseBlocks {

    private const val MAX_TRIES = 8

    fun place(block: Block, phase: Phase, fallback: Material = Material.STONE): Material {
        val placed = placeNow(block, phase, fallback)

        runNextTask {
            if (block.type.isAir) block.type = fallback
        }

        return placed
    }

    private fun placeNow(block: Block, phase: Phase, fallback: Material): Material {
        repeat(MAX_TRIES) {
            val material = phase.blockPool.getRandomByWeight() ?: fallback
            if (tryPlace(block, material, phase)) return material
        }

        block.type = fallback
        return fallback
    }

    private fun tryPlace(block: Block, material: Material, phase: Phase): Boolean {
        val support = supportFor(material, phase)

        if (support != null) {
            block.type = support
            runNextTask {
                if (block.type == support) placeDecoration(block.getRelative(BlockFace.UP), material)
            }
            return true
        }

        val data = runCatching { material.createBlockData() }.getOrNull() ?: return false
        if (!data.isSupported(block)) return false

        block.blockData = data

        if (block.type != material) return false

        if (material == Material.CHEST) fillChest(block, phase)

        return true
    }

    private fun placeDecoration(block: Block, material: Material) {
        if (!block.type.isAir && !block.isReplaceable) return

        val data = material.createBlockData()

        if (data !is Bisected) {
            block.setBlockData(data, false)
            return
        }

        val above = block.getRelative(BlockFace.UP)
        if (!above.type.isAir && !above.isReplaceable) return

        data.half = Bisected.Half.BOTTOM
        block.setBlockData(data, false)

        val topData = material.createBlockData() as Bisected
        topData.half = Bisected.Half.TOP
        above.setBlockData(topData, false)
    }

    private fun supportFor(material: Material, phase: Phase): Material? {
        val candidatos = supportCandidates(material) ?: return null

        candidatos.firstOrNull { phase.blockPool.containsKey(it) }?.let { return it }

        if (material == Material.RED_MUSHROOM || material == Material.BROWN_MUSHROOM) {
            solidFromPool(phase)?.let { return it }
        }

        return candidatos.first()
    }

    private fun supportCandidates(material: Material): List<Material>? = when {
        material == Material.CACTUS -> listOf(Material.SAND, Material.RED_SAND)

        material == Material.SUGAR_CANE ->
            listOf(Material.SAND, Material.RED_SAND, Material.DIRT, Material.GRASS_BLOCK)

        material == Material.DEAD_BUSH ->
            listOf(Material.SAND, Material.RED_SAND, Material.TERRACOTTA, Material.COARSE_DIRT)

        material == Material.NETHER_WART -> listOf(Material.SOUL_SAND, Material.SOUL_SOIL)

        material == Material.BAMBOO || material == Material.BAMBOO_SAPLING ->
            listOf(Material.DIRT, Material.GRASS_BLOCK, Material.PODZOL, Material.MOSS_BLOCK)

        material == Material.CRIMSON_FUNGUS || material == Material.CRIMSON_ROOTS ->
            listOf(Material.CRIMSON_NYLIUM, Material.NETHERRACK)

        material == Material.WARPED_FUNGUS || material == Material.WARPED_ROOTS ->
            listOf(Material.WARPED_NYLIUM, Material.NETHERRACK)

        material == Material.NETHER_SPROUTS ->
            listOf(Material.WARPED_NYLIUM, Material.CRIMSON_NYLIUM, Material.NETHERRACK)

        material == Material.RED_MUSHROOM || material == Material.BROWN_MUSHROOM ->
            listOf(Material.MYCELIUM, Material.PODZOL, Material.STONE)

        material == Material.MOSS_CARPET -> listOf(Material.MOSS_BLOCK)

        material == Material.PINK_PETALS -> GRAMA

        material == Material.SWEET_BERRY_BUSH -> GRAMA
        material == Material.SHORT_GRASS || material == Material.TALL_GRASS -> GRAMA
        material == Material.FERN || material == Material.LARGE_FERN -> GRAMA
        material == Material.PITCHER_PLANT || material == Material.TORCHFLOWER -> GRAMA

        Tag.FLOWERS.isTagged(material) -> GRAMA
        Tag.SAPLINGS.isTagged(material) -> listOf(Material.DIRT) + GRAMA
        Tag.CROPS.isTagged(material) -> listOf(Material.FARMLAND)

        else -> null
    }

    private val GRAMA = listOf(
        Material.GRASS_BLOCK,
        Material.DIRT,
        Material.PODZOL,
        Material.COARSE_DIRT,
        Material.ROOTED_DIRT,
        Material.MOSS_BLOCK,
        Material.MYCELIUM,
    )

    private fun solidFromPool(phase: Phase): Material? = phase.blockPool
        .filterKeys { it.isSolid && it.isOccluding && supportCandidates(it) == null }
        .getRandomByWeight()

    private const val MIN_DISTINCT_ITEMS = 2

    fun fillChest(block: Block, phase: Phase) {
        val table = phase.chestLoot ?: return
        if (table.entries.isEmpty()) return

        val state = block.getState(false) as? Chest ?: return
        val inventory = state.blockInventory
        inventory.clear()

        val sorteios = rollEntries(table)
        sorteios.forEach { entry ->
            val item = toItemStack(entry, phase) ?: return@forEach
            putSomewhere(inventory, item)
        }
    }

    private fun rollEntries(table: ChestLootTable): List<ChestLootEntry> {
        val rolls = randomBetween(table.minRolls, table.maxRolls)
            .coerceAtLeast(minOf(MIN_DISTINCT_ITEMS, table.entries.size))

        val escolhidas = mutableListOf<ChestLootEntry>()
        val restantes = table.entries.toMutableList()
        val distintos = minOf(MIN_DISTINCT_ITEMS, table.entries.size)

        repeat(minOf(distintos, rolls)) {
            val entry = restantes.associateWith { it.weight }.getRandomByWeight() ?: return@repeat
            restantes.remove(entry)
            escolhidas += entry
        }

        repeat(rolls - escolhidas.size) {
            val entry = table.entries.associateWith { it.weight }.getRandomByWeight() ?: return@repeat
            escolhidas += entry
        }

        return escolhidas
    }

    private fun putSomewhere(inventory: org.bukkit.inventory.Inventory, item: ItemStack) {
        val slot = ThreadLocalRandom.current().nextInt(inventory.size)

        if (inventory.getItem(slot) == null) {
            inventory.setItem(slot, item)
        } else {
            inventory.addItem(item)
        }
    }

    private fun toItemStack(entry: ChestLootEntry, phase: Phase): ItemStack? {
        val amount = randomBetween(entry.min, entry.max).coerceAtLeast(1)

        entry.texturaId?.let { id ->
            val item = ItemApi.create(id, amount)
            if (item == null) {
                Instance.logger.warning("[phase] [${phase.id}] item Textura inexistente no chest-loot: $id")
            }
            return item
        }

        val material = entry.material ?: return null
        return ItemStack(material, amount)
    }

    private fun randomBetween(min: Int, max: Int): Int {
        val low = minOf(min, max)
        val high = maxOf(min, max)
        if (low >= high) return low
        return ThreadLocalRandom.current().nextInt(low, high + 1)
    }
}
