package ocean_block.system.shop.shop

import org.bukkit.Material

data class Shop(
    val id: String,
    val prettyName: String,
    val icon: Material,
    val products: List<Product>,
    val description: List<String>,
)
