package ocean_block.system.shop.shop

import org.bukkit.Material

data class Product(
    val material: Material,
    val price: Double,
    val nameSingular: String,
    val namePlural: String,
)
