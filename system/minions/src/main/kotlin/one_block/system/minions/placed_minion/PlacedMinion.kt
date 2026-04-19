package one_block.system.minions.placed_minion

import one_block.libs.coordinate.SimpleLocation
import one_block.libs.item_builder.ItemSerializer
import one_block.system.minions.enums.MinionType
import one_block.system.minions.enums.MinionUpgrade
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import java.util.UUID

data class PlacedMinion(
    val uuid: UUID,
    val location: SimpleLocation,
    val type: MinionType,
    var energy: Int,
    val upgrades: MutableMap<String, Int> = mutableMapOf(),
    var enabled: Boolean = true,
    var toolBase64: String? = null,
) {

    companion object {
        const val MAX_ENERGY = 10_000
    }

    fun hasEnergy() = energy > 0

    fun hasEnoughEnergy(amount: Int) = energy >= amount

    fun addEnergy(amount: Int) {
        energy = (energy + amount).coerceAtMost(MAX_ENERGY)
    }

    fun consumeEnergy(amount: Int) {
        energy = (energy - amount).coerceAtLeast(0)
    }

    fun setUpgradeLevel(id: String, level: Int) {
        upgrades[id] = level
    }

    fun getUpgradeLevel(id: String, def: Int = 0): Int {
        return upgrades.getOrDefault(id, def)
    }

    fun setUpgradeLevel(upgrade: MinionUpgrade, level: Int) {
        upgrades[upgrade.id] = level
    }

    fun getUpgradeLevel(upgrade: MinionUpgrade, def: Int = 0): Int {
        return upgrades.getOrDefault(upgrade.id, def)
    }

    fun getTool(): ItemStack? =
        toolBase64?.let { runCatching { ItemSerializer.deserialize(it) }.getOrNull() }

    fun setTool(item: ItemStack?) {
        toolBase64 = item?.let { ItemSerializer.serialize(it) }
    }

    fun applyToolDurability(): Boolean {
        val tool = getTool() ?: return false
        val meta = tool.itemMeta as? Damageable ?: return false
        val unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING)
        if (kotlin.random.Random.nextInt(unbreaking + 1) != 0) return false
        val newDamage = meta.damage + 1
        if (newDamage >= tool.type.maxDurability) {
            setTool(null)
            return true
        }
        meta.damage = newDamage
        tool.itemMeta = meta
        setTool(tool)
        return false
    }
}