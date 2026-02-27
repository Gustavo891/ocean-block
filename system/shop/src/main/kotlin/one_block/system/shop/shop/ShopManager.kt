package one_block.system.shop.shop

import one_block.libs.instance.Instance
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object ShopManager {

    private val shops = mutableListOf<Shop>()

    fun toList() = shops.toMutableList()

    fun load() {
        shops.clear()
        val folder = File(Instance.dataFolder, "shop")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        folder.listFiles { file ->
            file.isFile && file.name.endsWith(".yml")
        }?.forEach { file ->

            val config = YamlConfiguration.loadConfiguration(file)
            val prettyName = config.getString("pretty-name")!!
            val icon = Material.matchMaterial(config.getString("icon")!!)!!
            val description = config.getStringList("description")

            val productsSection = config.getConfigurationSection("products")!!
            val products = mutableListOf<Product>()

            productsSection.getKeys(false).forEach {
                val p = productsSection.getConfigurationSection(it)
                if (p == null) {
                    Instance.logger.warning("Invalid product configuration in ${file.name}")
                    return@forEach
                }

                val rawMaterial = p.getString("material")
                if (rawMaterial == null) {
                    Instance.logger.warning("[shop] Invalid raw material in ${it}")
                    return@forEach
                }

                val material = Material.matchMaterial(rawMaterial)
                if (material == null) {
                    Instance.logger.warning("[shop] Invalid material in ${it}")
                    return@forEach
                }

                val price = p.getDouble("price")

                val nameSingular = p.getString("name-singular")!!
                val namePlural = p.getString("name-plural")!!

                products += Product(material, price, nameSingular, namePlural)
            }

            shops += Shop(file.nameWithoutExtension, prettyName, icon, products, description)
        }

        Instance.logger.info("Loaded ${shops.size} shops.")

    }
}