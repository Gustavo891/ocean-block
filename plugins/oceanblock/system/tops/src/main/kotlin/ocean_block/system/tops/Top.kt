package ocean_block.system.tops

import org.bukkit.Material

enum class Top(
    val prettyName: String,
    val iconMaterial: Material
) {
    Coins("<green>Coins",Material.EMERALD),
    Tokens("<yellow>Tokens", Material.GOLD_INGOT),
    Playtime("<aqua>Tempo online",Material.CLOCK)
}
