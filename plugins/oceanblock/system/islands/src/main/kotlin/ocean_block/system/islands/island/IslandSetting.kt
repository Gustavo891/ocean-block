package ocean_block.system.islands.island

import org.bukkit.Material

enum class IslandSetting(
    val displayName: String,
    val icon: Material,
    val enabledLore: String,
    val disabledLore: String,
    val defaultEnabled: Boolean = true,
) {
    MobSpawn(
        displayName = "Spawn de mobs",
        icon = Material.ZOMBIE_HEAD,
        enabledLore = "Mobs aparecem ao quebrar blocos.",
        disabledLore = "Mobs não aparecem ao quebrar blocos.",
        defaultEnabled = true,
    ),
    NaturalMobSpawn(
        displayName = "Spawn natural",
        icon = Material.SPAWNER,
        enabledLore = "Mobs aparecem naturalmente na ilha.",
        disabledLore = "Mobs não aparecem naturalmente na ilha.",
        defaultEnabled = true,
    ),
    Redstone(
        displayName = "Redstone",
        icon = Material.REDSTONE_TORCH,
        enabledLore = "Circuitos de redstone funcionam na ilha.",
        disabledLore = "Circuitos de redstone não propagam sinal na ilha.",
        defaultEnabled = true,
    ),
    LeafDecay(
        displayName = "Queda de folhas",
        icon = Material.OAK_LEAVES,
        enabledLore = "Folhas somem quando a árvore é cortada.",
        disabledLore = "Folhas não somem quando a árvore é cortada.",
        defaultEnabled = true,
    ),
    LiquidFlow(
        displayName = "Fluxo de líquidos",
        icon = Material.WATER_BUCKET,
        enabledLore = "Água e lava se espalham na ilha.",
        disabledLore = "Água e lava não se espalham na ilha.",
        defaultEnabled = true,
    ),
    FireSpread(
        displayName = "Propagação de fogo",
        icon = Material.CAMPFIRE,
        enabledLore = "O fogo se espalha e queima blocos da ilha.",
        disabledLore = "O fogo não se espalha nem queima blocos da ilha.",
        defaultEnabled = true,
    ),
    MobGriefing(
        displayName = "Mobs alteram blocos",
        icon = Material.ENDER_PEARL,
        enabledLore = "Mobs como endermen podem mover blocos.",
        disabledLore = "Mobs não podem mover blocos da ilha.",
        defaultEnabled = true,
    );
}
