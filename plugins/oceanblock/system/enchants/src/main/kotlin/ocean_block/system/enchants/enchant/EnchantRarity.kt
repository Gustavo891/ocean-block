package ocean_block.system.enchants.enchant

enum class EnchantRarity(
    val prettyName: String,
    val color: String,
    val successChance: Double,
    val destroyOnFailure: Boolean,
) {

    Common("Comum", "<white>", 1.0, false),
    Rare("Raro", "<aqua>", 0.85, false),
    Epic("Épico", "<light_purple>", 0.60, true),
    Legendary("Lendário", "<gold>", 0.35, true);

    fun formatted(): String = "$color$prettyName"
}
