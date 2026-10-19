package ocean_block.system.craft.recipe

enum class RecipeCategory(
    val displayName: String,
    val icon: String,
    val description: String,
) {
    FERRAMENTAS(
        displayName = "Ferramentas",
        icon = "ui:pickaxe",
        description = "Equipamentos para trabalhar no mar.",
    ),
    COLETA(
        displayName = "Coleta",
        icon = "ui:chest",
        description = "Itens que recolhem o que a correnteza traz.",
    ),
}
