package one_block.system.sell.manager

import one_block.libs.instance.Instance
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object SellManager {

    private val prices = mutableMapOf<Material, Double>()

    fun load() {
        prices.clear()

        val file = File(Instance.dataFolder, "sell.yml")
        if (!file.exists()) {
            Instance.saveResource("sell.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("items")
        if (section == null) {
            Instance.logger.warning("sell.yml is missing 'items' section.")
            return
        }

        section.getKeys(false).forEach { key ->
            val material = Material.matchMaterial(key)
            if (material == null) {
                Instance.logger.warning("[sell] Invalid material for '$key'")
                return@forEach
            }

            val price = section.getDouble(key)
            if (price <= 0) {
                Instance.logger.warning("[sell] Invalid price for '$key'")
                return@forEach
            }

            prices[material] = price
        }

        Instance.logger.info("[sell] Loaded ${prices.size} sellable items.")
    }

    fun getPrice(material: Material): Double = prices[material] ?: 0.0
}