package one_block.system.craft

import org.bukkit.Material

enum class CraftStation(val prettyName: String) {
    CRAFT("Fabricações"),
    FORJA("Forja"),
}

enum class CraftCategory(
    val prettyName: String,
    val icon: Material,
    val station: CraftStation,
    val description: String,
) {
    MINERACAO(
        prettyName = "Mineração",
        icon = Material.DIAMOND_PICKAXE,
        station = CraftStation.CRAFT,
        description = "Equipamento de picareta e escavação.",
    ),
    AGRICULTURA(
        prettyName = "Agricultura",
        icon = Material.DIAMOND_HOE,
        station = CraftStation.CRAFT,
        description = "Ferramentas de plantio e colheita.",
    ),
    COMBATE(
        prettyName = "Combate",
        icon = Material.DIAMOND_SWORD,
        station = CraftStation.CRAFT,
        description = "Armas e proteção contra mobs.",
    ),
    COLETA(
        prettyName = "Coleta",
        icon = Material.DIAMOND_AXE,
        station = CraftStation.CRAFT,
        description = "Machados e itens de extração de madeira.",
    ),
    ENCANTAMENTO(
        prettyName = "Encantamento",
        icon = Material.ENCHANTING_TABLE,
        station = CraftStation.CRAFT,
        description = "Livros e catalisadores de encantamento.",
    ),
    ESPECIAIS(
        prettyName = "Especiais",
        icon = Material.NETHER_STAR,
        station = CraftStation.CRAFT,
        description = "Receitas únicas que não cabem em outra categoria.",
    ),
    MINERIOS(
        prettyName = "Minérios",
        icon = Material.RAW_IRON,
        station = CraftStation.FORJA,
        description = "Refino de metal bruto em lingote.",
    ),
    LIGAS(
        prettyName = "Ligas",
        icon = Material.IRON_INGOT,
        station = CraftStation.FORJA,
        description = "Combinação de metais já refinados.",
    ),
}
