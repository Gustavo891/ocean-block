package ocean_block.system.craft.recipe

enum class RecipeCategory(
    val displayName: String,
    val material: String,
    val description: String,
) {
    FERRAMENTAS(
        displayName = "Ferramentas",
        material = "iron_pickaxe",
        description = "Equipamentos para trabalhar no mar.",
    ),
    COLETA(
        displayName = "Coleta",
        material = "chest",
        description = "Itens que recolhem o que a correnteza traz.",
    ),
    PROCESSAMENTO(
        displayName = "Estações",
        material = "composter",
        description = "Estações que transformam o que o mar traz.",
    ),
    MAQUINAS(
        displayName = "Máquinas",
        material = "observer",
        description = "Máquinas que trabalham sozinhas com energia.",
    ),
}
