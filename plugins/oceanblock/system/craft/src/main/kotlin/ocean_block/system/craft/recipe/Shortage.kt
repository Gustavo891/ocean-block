package ocean_block.system.craft.recipe

data class Shortage(
    val ingredient: Ingredient,
    val have: Int,
    val missing: Int,
)
