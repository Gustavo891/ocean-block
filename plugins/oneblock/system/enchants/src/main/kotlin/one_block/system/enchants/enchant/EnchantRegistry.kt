package one_block.system.enchants.enchant

import one_block.libs.instance.Instance

object EnchantRegistry {

    private val enchants = linkedMapOf<String, CustomEnchant>()

    fun register(enchant: CustomEnchant) {
        val existing = enchants.put(enchant.id, enchant)
        if (existing != null) {
            Instance.logger.warning("[enchants] Encantamento '${enchant.id}' registrado duas vezes; o anterior foi substituído.")
        }
    }

    fun find(id: String): CustomEnchant? = enchants[id.lowercase()]

    fun all(): List<CustomEnchant> = enchants.values.toList()

    fun applicableTo(material: org.bukkit.Material): List<CustomEnchant> =
        enchants.values.filter { enchant -> enchant.targets.any { it.matches(material) } }
}
