package one_block.trader.model

enum class ItemRarity(
    val displayName: String,
    val color: String,
    val weight: Int,
) {
    COMMON("Comum", "<gray>", 60),
    UNCOMMON("Incomum", "<green>", 25),
    RARE("Raro", "<aqua>", 10),
    EPIC("Épico", "<light_purple>", 4),
    LEGENDARY("Lendário", "<gold>", 1),
}